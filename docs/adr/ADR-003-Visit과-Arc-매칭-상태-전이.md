# ADR-003. Visit과 Arc 및 Visit Memory 상태 전이

- 상태. Accepted
- 결정일. 2026-08-15
- 범위. 고객 온보딩, 직원 연결, 구매 판단, Arc·Visit Memory 생성 공개와 고객 방문 종료

## 문맥

IA는 직원 추천 고객과 혼자 보기 고객을 같은 Visit에서 처리한다. 직원은 오프라인 응대 중 구매 여부를 확정하고, 구매 고객에게는 Arc 생성이 성공하면 즉시 공개한다. 미구매 고객에게는 직원이 Visit Memory를 생성·공유하고, 고객은 웹 알림에서 기록을 확인한다. OpenAI 실패 시 직원은 같은 초안을 재생성할 수 있어야 한다.

## 결정

### Visit 상태

- `ONBOARDING`. 고객이 초기 설정을 진행 중이다.
- `WAITING_FOR_STAFF`. 직원 추천을 선택하고 직원 연결을 기다린다.
- `ACTIVE`. 직원이 연결되었거나 혼자 보기 고객이 응대 중이다.
- `ARC_IN_PROGRESS`. 직원이 구매를 확정하고 Arc를 생성·공유하는 중이다.
- `VISIT_MEMORY_IN_PROGRESS`. 직원이 미구매를 확정하고 Visit Memory를 생성하는 중이다.
- `COMPLETED`. Arc 생성 성공 또는 Visit Memory 공유가 완료되어 방문이 정상 종료되었다.
- `CANCELED`. 고객 이탈 또는 직원 취소로 종료되었다.

### Visit 전이

```text
ONBOARDING
  ├─ STAFF_RECOMMENDATION 선택 → WAITING_FOR_STAFF
  └─ SELF_GUIDED 선택          → ACTIVE

WAITING_FOR_STAFF
  └─ 직원 연결                  → ACTIVE

ACTIVE
  ├─ 직원 구매 확정             → ARC_IN_PROGRESS
  ├─ 직원 미구매 확정            → VISIT_MEMORY_IN_PROGRESS
  └─ 취소                       → CANCELED

ARC_IN_PROGRESS
  ├─ Arc 생성 성공·고객 공개       → COMPLETED
  └─ 취소                       → CANCELED

VISIT_MEMORY_IN_PROGRESS
  ├─ 생성 성공·최종 저장         → COMPLETED
  ├─ 생성 실패                   → 상태 유지, 재시도 가능
  └─ 취소                       → CANCELED
```

혼자 보기 고객의 구매 또는 미구매 판단 전에는 직원이 `assignStaff`로 담당 직원을 연결한다. 구매 판단은 담당 직원만 수행할 수 있다.

### Arc와 ArcRevision 상태

```text
Arc
DRAFT → SHARED

ArcRevision
GENERATING → READY
            └→ FAILED → 새 리비전 생성으로 재시도
```

- 직원은 전체 입력 스냅샷과 템플릿 버전을 가진 `ArcRevision`을 생성한다.
- OpenAI 결과는 `generatedContent`에 저장하고, 최초 READY 리비전은 생성 성공 트랜잭션에서 즉시 고객에게 공개한다.
- Arc 생성 성공 시 공개 리비전의 ID를 `sharedRevisionId`에 기록하고 Visit을 `COMPLETED`로 전환한다.
- `SHARED` Arc도 기존 입력 재생성 또는 수정 입력 재생성을 허용하며, 새 READY 리비전이 생성되면 `sharedRevisionId`를 교체한다.
- 재생성 실패 시 기존 공개 리비전을 유지한다.
- 수정 요청은 기존 리비전을 덮어쓰지 않고 새 리비전으로 보관한다.
- 기존 `FINALIZED` Arc 데이터는 고객 조회 호환을 위해 유지하지만, 신규 Arc 흐름에서는 `FINALIZED`로 전환하지 않는다.

### Visit Memory 생성

- Visit Memory는 방문당 하나의 초안만 유지하며 재생성 시 같은 레코드의 입력과 결과를 갱신한다.
- 직원의 공유 동작은 `READY` Visit Memory를 `FINALIZED`로 바꾸고 Visit을 `COMPLETED`로 전환한다.
- 생성 실패 시 Visit Memory를 `FAILED`로 남기고 Visit은 `VISIT_MEMORY_IN_PROGRESS` 상태로 유지한다.
- 직원은 FAILED Visit Memory를 다시 `GENERATING`으로 전환해 재시도할 수 있다.
- 공유 트랜잭션에서 고객 웹 알림을 함께 저장한다. 알림은 최신순 전체 목록으로 조회하고 별도 읽음 처리 API를 제공한다.
- 고객은 알림의 `resourceId`로 최종 저장된 Visit Memory 상세에 진입하며, 직원 입력 원문은 조회하지 않는다.

## 전이 책임

- `Visit`, `Arc`, `ArcRevision`, `VisitMemory` 엔티티는 자신의 상태 전이와 필수 시각을 검증한다.
- Application Service는 직원·방문·매장의 일치 여부와 인증 주체를 검증하고 구매·미구매 판단을 호출한다.
- 외부 OpenAI 호출은 데이터베이스 트랜잭션 밖에서 수행하고 결과 상태 변경은 별도 트랜잭션으로 저장한다.
- Arc 생성 성공·공개와 Visit 완료, Visit Memory 공유와 Visit 완료는 각각 하나의 애플리케이션 트랜잭션으로 처리한다.

## 대안

### 고객이 Arc를 최종 저장

고객에게 별도 최종 저장 동작을 요구하지 않고, 직원의 Arc 생성 성공 시 고객에게 즉시 공개하고 Visit을 완료한다. 이후 수정·재생성은 같은 Arc의 새 리비전으로 처리한다.

### OpenAI 실패 시 Visit 완료 처리

실패한 결과가 고객 이력으로 남을 수 있어 Visit 완료를 보류하고 FAILED 상태에서 재시도하도록 결정한다.

### 리비전 없이 현재 Arc만 덮어쓰기

수정 이력과 생성 시점의 입력을 잃게 된다. 전체 입력 스냅샷과 결과를 리비전에 보존해 고객별 수정 이력을 직원이 확인할 수 있게 한다.

## 결과

### 장점

- 두 응대 방식이 하나의 Visit 상태 모델로 수렴한다.
- 구매 여부를 확정한 담당 직원을 기록하고 중복 구매·Arc·Visit Memory를 데이터베이스 유일 제약으로 방어할 수 있다.
- OpenAI 실패를 숨기지 않고 재시도 가능한 상태로 보존한다.
- Arc 입력과 결과의 시점별 스냅샷을 유지하면서 최신 성공 리비전을 고객에게 제공한다.
- Visit Memory 공유와 동시에 고객에게 웹 알림을 남겨 별도 외부 푸시 인프라 없이 기록 확인을 유도한다.

### 비용과 제한

- Application Service가 여러 Aggregate의 같은 매장·담당 직원 규칙을 검증해야 한다.
- Visit은 최초 Arc 생성 성공 시 완료되므로 이후 직원 재생성은 완료된 Visit에 연결된 Arc를 통해 수행한다.
- OpenAI 호출 재시도는 직원의 단일 초안 재생성으로 처리한다.
- 외부 Push, SSE, WebSocket은 사용하지 않고 고객 웹 알림 목록과 읽음 상태로 범위를 제한한다.

## 구현 근거

- `domain/visit/entity/Visit.java`
- `domain/visit/entity/VisitStatus.java`
- `domain/visit/entity/PurchaseDecision.java`
- `domain/arc/entity/Arc.java`
- `domain/arc/entity/ArcRevision.java`
- `domain/visitmemory/entity/VisitMemory.java`
- `domain/notification/entity/CustomerNotification.java`
- `application/customer/CustomerNotificationService.java`
- `application/customer/CustomerVisitMemoryQueryService.java`
- `infrastructure/web/customer/CustomerNotificationController.java`
- `infrastructure/web/customer/CustomerVisitMemoryController.java`
- `V2__rebuild_domain_for_ia.sql`
- `V5__create_customer_notifications.sql`
