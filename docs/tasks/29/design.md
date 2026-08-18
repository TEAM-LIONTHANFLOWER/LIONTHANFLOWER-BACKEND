# 이슈 29 설계

## 목표

직원이 미구매 방문의 Visit Memory를 생성·미리보기·재생성한 뒤 고객에게 공유하고, 고객은 웹 알림에서 최종 기록을 조회한다. 구매 방문은 고객이 공유 Arc를 최종 저장하면 완료한다.

## Visit Memory 모델

- Visit Memory는 방문당 하나만 유지하며 재생성 이력을 별도 엔티티로 보존하지 않는다.
- 상태는 `DRAFT → GENERATING → READY → FINALIZED`이며 생성 실패는 `FAILED`로 기록한다.
- `READY`와 `FAILED`에서 기존 또는 수정된 입력으로 재생성할 수 있다.
- OpenAI 결과는 최소 구조인 `{ "summary": "..." }`로 저장한다.
- OpenAI 호출은 트랜잭션 밖에서 실행하고 상태 변경만 짧은 트랜잭션으로 처리한다.
- 공유 전 초안은 직원에게만 노출한다.

## 직원 API

- `POST /api/staff/visits/{visitId}/visit-memories`에서 최초 생성한다.
- `GET /api/staff/visit-memories/{visitMemoryId}`에서 현재 초안을 조회한다.
- `POST /api/staff/visit-memories/{visitMemoryId}/regenerations`에서 기존 또는 수정 입력으로 재생성한다.
- `POST /api/staff/visit-memories/{visitMemoryId}/share`에서 고객에게 공유한다.
- Arc와 동일하게 `StaffVisitMemoryService`가 외부 호출을 조정하고 `StaffVisitMemoryStateService`가 트랜잭션 상태를 관리한다.

## 공유와 알림

- 공유 시 `VisitMemory.FINALIZED`, `Visit.COMPLETED`, 고객 알림 생성을 하나의 트랜잭션으로 처리한다.
- 고객 알림은 웹 화면 내부 알림이며 외부 Push, SSE와 WebSocket은 사용하지 않는다.
- 알림은 `customerId`, `type`, `resourceId`, `message`, `readAt`을 저장한다.
- `resourceId`는 Visit Memory ID이며 동일 Visit Memory 알림은 하나만 생성한다.
- 고객은 알림 전체를 최신순으로 조회하고 별도 PATCH API로 멱등하게 읽음 처리한다.

## 고객 공개 API

- `GET /api/customers/notifications`에서 알림 전체를 최신순으로 조회한다.
- `PATCH /api/customers/notifications/{notificationId}/read`에서 읽음 처리한다.
- `GET /api/customers/visit-memories/{visitMemoryId}`에서 본인의 `FINALIZED` Visit Memory만 조회한다.
- 고객 Visit Memory 목록 API는 추가하지 않는다.
- 고객 상세 응답에는 생성 요약, 매장 정보와 최종 저장 시각을 포함하며 직원 입력 원문은 노출하지 않는다.

## 고객 Arc 최종 저장

- `POST /api/customers/arcs/{arcId}/finalize`에서 고객 본인의 `SHARED` Arc를 최종 저장한다.
- Arc 최종 저장과 Visit 완료를 하나의 트랜잭션으로 처리한다.
- 동일 고객의 이미 완료된 Arc 요청은 기존 결과를 반환한다.

## 데이터베이스와 문서

- `V5__create_customer_notifications.sql`로 고객 알림 테이블과 인덱스·유일 제약을 추가한다.
- 기존 Visit Memory 테이블은 변경하지 않고 enum 문자열로 `READY`를 저장한다.
- ADR-003을 직원 미리보기·공유·웹 알림 흐름으로 갱신한다.

