# ADR-003. Visit과 Arc 매칭 상태 전이

- 상태. Accepted
- 결정일. 2026-08-14
- 범위. 고객 온보딩, 직원 매칭, 구매 확인, Arc 확정과 방문 종료

## 문맥

고객의 응대 방식에 따라 매칭 흐름이 달라진다. 직원 추천을 원하는 고객은 직원 목록에서 선택되어야 하고, 혼자 보기를 선택한 고객은 구매한 경우에만 직원이 목록에서 선택해 Arc를 발급받을 수 있다.

Arc는 직원이 생성한 뒤 고객이 최종 이미지를 확인하고, 직원이 방문을 종료할 때 최종 저장된다. 따라서 Visit과 Arc의 상태를 분리하면서도 허용된 전이를 명확히 해야 한다.

## 결정

### Visit 상태

Visit은 다음 상태를 사용한다.

- `ONBOARDING`. 고객이 초기 설정을 진행 중이다.
- `WAITING_FOR_STAFF`. 직원 추천을 선택하고 직원 연결을 기다린다.
- `MATCHED`. 직원 추천 고객이 직원과 연결되어 오프라인 응대 중이다.
- `SELF_GUIDED`. 고객이 혼자 상품을 보고 있다.
- `ARC_IN_PROGRESS`. 구매 확인 후 Arc 생성·수정·고객 확정을 진행 중이다.
- `COMPLETED`. 방문이 정상 종료되었다.
- `CANCELED`. 고객 이탈 또는 직원 취소로 종료되었다.

### 직원 추천 흐름

```text
ONBOARDING
  -- STAFF_RECOMMENDATION 선택 --> WAITING_FOR_STAFF
  -- 직원이 고객 선택 -----------> MATCHED
  -- 직원이 구매 확인 -----------> ARC_IN_PROGRESS
  -- 고객이 Arc 확정, 직원 종료 --> COMPLETED
```

### 혼자 보기 흐름

```text
ONBOARDING
  -- SELF_GUIDED 선택 -----------> SELF_GUIDED
  -- 구매 후 직원이 고객 선택 ----> ARC_IN_PROGRESS
  -- 고객이 Arc 확정, 직원 종료 --> COMPLETED
```

혼자 보기 고객이 구매하지 않은 경우에는 `SELF_GUIDED`에서 `COMPLETED`로 종료할 수 있다. 고객 이탈이나 직원 취소는 종료되지 않은 진행 상태에서 `CANCELED`로 전환한다.

### Arc 상태

Arc는 다음 상태를 사용한다.

- `DRAFT`. 직원이 생성하거나 수정 중이다.
- `CONFIRMED`. 고객이 최종 이미지를 확인했다.
- `FINALIZED`. 직원이 Arc 저장과 방문 종료를 완료했다.

Arc 전이는 `DRAFT → CONFIRMED → FINALIZED`만 허용한다. 방문당 Arc는 최대 한 건이며, 수정 시 기존 이미지 객체 키를 새 객체 키로 교체하고 수정 이력은 별도로 저장하지 않는다.

### 전이 책임

- `Visit` 엔티티는 자신의 상태 전이만 검증한다.
- `Arc` 엔티티는 자신의 상태 전이만 검증한다.
- Application Service는 직원·단말·방문이 같은 매장인지 확인하고, 구매 확인 권한과 Arc 생성 권한을 조정한다.
- 매칭 종료 Application Service는 Arc가 `CONFIRMED`인지 확인한 뒤 Arc의 `FINALIZED` 전환과 Visit의 `COMPLETED` 전환을 하나의 트랜잭션으로 처리한다.
- 직원의 중복 고객 선택은 Visit의 낙관적 잠금 `version`으로 방어한다.

## 대안

### SELF_GUIDED 고객은 Arc를 발급받을 수 없도록 제한

직원 추천 흐름은 단순해지지만, 혼자 보기를 선택한 고객이 구매 후 Arc를 받을 수 없다는 서비스 요구사항을 충족하지 못한다. 구매 후 직원 단말 목록에서 고객을 선택하는 경로를 별도로 허용한다.

### 고객 매칭 승인을 별도 상태로 추가

직원이 고객을 선택한 뒤 고객의 추가 승인을 받으면 오선택을 줄일 수 있지만, 매장 오프라인 응대 흐름이 길어지고 현재 MVP 화면·API 요구와 맞지 않는다. 직원 선택 즉시 매칭되도록 결정한다.

### Arc 수정 이력과 여러 버전 보관

고객 피드백 과정의 복구에는 유리하지만, MVP에서 필요한 것은 최종본뿐이다. 데이터 모델과 객체 스토리지 비용을 줄이기 위해 현재 이미지 객체 키만 유지한다.

## 결과

### 장점

- 직원 추천과 혼자 보기 고객의 흐름을 같은 Visit 모델로 표현할 수 있다.
- 구매 확인 전에는 Arc 생성 상태로 진입할 수 없다.
- 고객 확정과 직원 종료 순서를 상태로 강제할 수 있다.
- 방문당 최종 Arc 한 건이라는 조회 규칙이 데이터베이스 유니크 제약조건으로 보장된다.

### 비용과 제한

- Visit과 Arc를 함께 종료하는 Application Service의 트랜잭션 구현이 필요하다.
- Arc 수정 이력이 없어 과거 이미지 복구와 변경 비교를 제공할 수 없다.
- 고객이 브라우저 쿠키를 잃으면 이전 Visit을 다시 이어갈 수 없다.
- 구매 여부는 외부 결제 시스템이 아니라 직원의 오프라인 확인 입력을 신뢰한다.

## 구현 근거

- `domain/visit/entity/Visit.java`
- `domain/visit/entity/VisitStatus.java`
- `domain/arc/entity/Arc.java`
- `domain/arc/entity/ArcStatus.java`
- `domain/visit/entity/VisitTest.java`
- `domain/arc/entity/ArcTest.java`
- `V1__create_initial_domain_tables.sql`

