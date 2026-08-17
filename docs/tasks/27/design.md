# 직원 Arc 생성·재생성 및 고객 공유 설계

## 목표

직원이 방문 고객의 입력 정보를 바탕으로 OpenAI를 통해 Arc를 생성하고, 생성 결과를 확인한 뒤 입력 수정·재생성 또는 고객 공유를 선택할 수 있도록 한다.

## 범위

- `ACTIVE` 방문에서 직원이 Arc 생성을 시작한다.
- Solo 방문에 담당 직원이 없으면 최초 Arc 생성 또는 Visit Memory 저장 요청을 보낸 현재 직원을 담당자로 배정한다.
- With 방문은 기존에 배정된 담당 직원만 처리한다.
- 기존 `Purchase`, `PurchaseItem`, `Visit` 구매 상태 전이는 유지하고 Arc 생성 내부에서 처리한다.
- Arc 입력은 구매 정보, 고객 선호, 응대 특성, 직원 관찰 정보를 포함한다.
- OpenAI 생성 결과는 `ArcRevision`으로 이력을 보존한다.
- `Initial setup`과 `Arc 수정`은 같은 입력 조회·수정 흐름을 사용한다.
- 생성 결과 직접 편집은 제공하지 않는다.
- `Arc 전송`을 눌렀을 때만 선택한 `READY` 리비전을 `SHARED` 상태로 공유한다.
- 고객 알림은 보내지 않는다.
- 고객 보유 Arc 수는 `SHARED`, `FINALIZED` Arc만 센다.

## 상태 흐름

```text
ACTIVE 방문
  └─ Arc 생성 요청
       ├─ Arc DRAFT + ArcRevision GENERATING 저장
       ├─ OpenAI 성공 → ArcRevision READY
       ├─ OpenAI 실패 → ArcRevision FAILED
       ├─ 다시 생성하기 → 기존 입력으로 새 리비전 생성
       ├─ Initial setup / Arc 수정 → 입력 수정 후 새 리비전 생성
       └─ Arc 전송 → 선택한 READY 리비전 공유, Arc SHARED
```

## API 방향

- 방문 고객 목록 응답에 `arcCount`, `additionalRequest`를 추가한다.
- Arc 생성 요청은 `POST /api/staff/visits/{visitId}/arcs`로 시작한다.
- 직원은 생성된 Arc와 현재 입력을 조회할 수 있어야 한다.
- 재생성 및 입력 수정은 새 `ArcRevision`을 생성하는 하나의 흐름으로 처리한다.
- 공유는 선택한 리비전을 명시하여 `POST /api/staff/arcs/{arcId}/revisions/{revisionId}/share`로 처리한다.

## OpenAI 연동

- Spring `RestClient`로 OpenAI Responses API를 호출한다.
- API 키와 모델명은 `OPENAI_API_KEY`, `OPENAI_MODEL` 환경 변수로 관리한다.
- 외부 호출은 데이터베이스 트랜잭션 밖에서 수행한다.
- 응답은 `ArcGeneratedContent` 형식으로 검증한 뒤 저장한다.
- 테스트에서는 OpenAI 포트를 대체하여 외부 네트워크를 호출하지 않는다.

## 권한과 예외

- `staffToken` 인증과 직원·방문 매장 일치 여부를 검증한다.
- With 방문은 현재 담당 직원이 아니면 거부한다.
- Solo 방문은 담당 직원이 없을 때 현재 직원이 선점한다.
- 다른 직원이 이미 선점한 Solo 방문은 `409 Conflict`로 처리한다.
- `READY`가 아닌 리비전 공유는 `409 Conflict`로 처리한다.
- OpenAI 실패는 `FAILED` 리비전과 실패 코드를 남기고 재시도 가능하게 한다.

## 제외 범위

- 직원 프로필 수정.
- 고객 알림 발송.
- 생성된 Arc 문구 직접 편집.
- 별도의 구매 여부 결정 API.
