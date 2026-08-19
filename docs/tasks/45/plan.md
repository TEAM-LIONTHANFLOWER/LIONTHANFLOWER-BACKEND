# Arc 생성 즉시 공개 및 재생성 흐름 단순화 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task with verification checkpoints.

**Goal:** 최초 Arc 생성 성공 시 고객에게 즉시 공개하고 Visit을 완료하며, 공개된 Arc도 기존 입력 재생성 또는 수정 입력 재생성을 통해 최신 성공 리비전으로 갱신한다.

**Architecture:** OpenAI 호출은 현재처럼 트랜잭션 밖의 `StaffArcService`에서 수행한다. 생성 결과를 저장하는 `StaffArcStateService.complete` 트랜잭션이 READY 리비전을 Arc의 공개 리비전으로 지정하고, 최초 생성일 때만 Visit을 COMPLETED로 전환한다. 이후 SHARED Arc의 재생성은 `Arc.reshare`로 공개 리비전 포인터만 교체하며 실패 시 기존 포인터를 유지한다.

**Tech Stack:** Java 21, Spring Boot 4.1, Spring Web MVC, Spring Data JPA, Gradle, JUnit 5, Mockito, Flyway.

## Global Constraints

- 작업 브랜치는 `feat/45`이며 기준 브랜치는 `main`이다.
- 작업 범위는 GitHub 이슈 #45에 한정한다.
- ArcRevision 이력은 삭제하거나 덮어쓰지 않고 새 리비전으로 보존한다.
- OpenAI 실패 시 고객에게 노출 중인 기존 READY 리비전을 변경하지 않는다.
- 새 소스 파일은 만들지 않으며, 기존 소스 파일의 첫 줄 역할 주석을 유지한다.
- 코드 또는 설정 변경 후 `./gradlew test --stacktrace --no-daemon`을 실행한다.

## Files and Responsibilities

- Modify `src/main/java/com/lionthanflower/application/staff/StaffArcStateService.java`: 최초 READY 결과 공개, Visit 완료, SHARED 재생성 허용, 공유 리비전 교체.
- Modify `src/main/java/com/lionthanflower/application/staff/StaffArcService.java`: 직원 공유 위임 메서드 제거.
- Modify `src/main/java/com/lionthanflower/controller/staff/StaffArcController.java`: 직원 공유 엔드포인트 제거.
- Modify `src/main/java/com/lionthanflower/infrastructure/web/customer/CustomerArcController.java`: 고객 최종 저장 엔드포인트와 의존성 제거.
- Delete `src/main/java/com/lionthanflower/application/customer/CustomerArcCommandService.java`: 더 이상 사용하지 않는 고객 최종 저장 애플리케이션 서비스 제거.
- Modify `src/test/java/com/lionthanflower/application/staff/StaffArcStateServiceTest.java`: 신규 공개와 SHARED 재생성 상태 전이 검증.
- Modify `src/test/java/com/lionthanflower/application/staff/StaffArcServiceTest.java`: 공유 위임 제거에 맞춘 서비스 테스트 정리.
- Modify `src/test/java/com/lionthanflower/controller/staff/StaffArcControllerTest.java`: 직원 공유 HTTP 테스트 제거 및 생성 응답 계약 보강.
- Delete `src/test/java/com/lionthanflower/application/customer/CustomerArcCommandServiceTest.java`: 제거된 고객 최종 저장 서비스 테스트 삭제.
- Modify `src/test/java/com/lionthanflower/infrastructure/web/customer/CustomerArcControllerTest.java`: 고객 최종 저장 HTTP 테스트 제거 및 생성자 의존성 갱신.
- Modify `docs/adr/ADR-003-Visit과-Arc-매칭-상태-전이.md`: Arc 공개와 Visit 완료 결정 갱신.

### Task 1: 최초 생성 성공 시 Arc 공개와 Visit 완료

**Files:** `StaffArcStateService.java`, `StaffArcStateServiceTest.java`.

- [x] `complete(revisionId, content)` 성공 경로에서 Revision을 READY로 바꾼 뒤 Arc를 조회한다.
- [x] Arc가 DRAFT이면 고객별 공개 Arc 수를 기준으로 다음 `arcNumber`를 계산하고 `arc.shareFirst(revision, now, nextArcNumber)`를 호출한다.
- [x] 최초 공개에 사용한 Arc를 `saveAndFlush`하여 `sharedRevisionId`, `sharedAt`, `arcNumber`, `SHARED` 상태를 저장한다.
- [x] Arc의 Visit을 ID로 조회하고 상태가 `ARC_IN_PROGRESS`일 때만 `visit.complete(now)`를 호출한다.
- [x] Visit 완료와 Arc 공개가 같은 `@Transactional` 메서드에서 실행되도록 유지한다.
- [x] 최초 생성 성공 테스트에서 응답의 `arcStatus`가 `SHARED`, Revision 상태가 `READY`, Arc 번호가 1, Visit 상태가 `COMPLETED`인지 검증한다.
- [x] 최초 생성 실패 테스트에서 `fail` 호출 후 Arc 상태가 DRAFT이고 Visit 상태가 `ARC_IN_PROGRESS`인지 검증한다.
- [x] 고객별 기존 SHARED/FINALIZED Arc가 있는 경우 새 Arc 번호가 기존 공개 수 + 1인지 검증한다.

### Task 2: SHARED Arc의 수정·재생성 및 공개 리비전 교체

**Files:** `StaffArcStateService.java`, `StaffArcStateServiceTest.java`, `Arc.java` tests if required.

- [x] `prepareRevision`의 상태 조건을 DRAFT와 SHARED로 확장한다.
- [x] 기존 입력을 재사용하는 요청과 수정된 `inputSnapshot` 요청이 현재 동작처럼 각각 이전 스냅샷 또는 요청 스냅샷을 사용하도록 유지한다.
- [x] `complete` 성공 경로에서 Arc가 SHARED이면 `arc.reshare(revision, now)`를 호출한다.
- [x] SHARED 재생성에서는 Arc 번호를 다시 계산하지 않고 기존 `arcNumber`를 유지한다.
- [x] SHARED 재생성 성공 시 `sharedRevisionId`, `sharedAt`이 새 READY 리비전을 가리키는지 검증한다.
- [x] SHARED 재생성 실패 시 `fail`만 수행하고 기존 `sharedRevisionId` 및 고객 노출 리비전이 유지되는지 검증한다.
- [x] SHARED Arc 재생성 후 Visit이 이미 COMPLETED인 상태를 유지하고 중복 완료 처리를 하지 않는지 검증한다.
- [x] DRAFT와 SHARED 외 상태의 재생성은 기존 `NOT_ASSIGNABLE` 오류를 반환하는지 검증한다.

### Task 3: 불필요한 직원 공유·고객 최종 저장 API 제거

**Files:** `StaffArcService.java`, `StaffArcController.java`, `CustomerArcController.java`, `CustomerArcCommandService.java`, related tests.

- [x] `StaffArcStateService.share`, `StaffArcService.share`, `StaffArcController.share`를 제거한다.
- [x] `POST /api/staff/arcs/{arcId}/revisions/{revisionId}/share` 매핑과 관련 컨트롤러 테스트를 제거한다.
- [x] `CustomerArcController`에서 `CustomerArcCommandService` 의존성, finalize 매핑, 응답 record를 제거한다.
- [x] `CustomerArcCommandService`와 전용 테스트를 삭제한다.
- [x] 고객 Arc 목록·상세 API는 기존처럼 `SHARED` 및 레거시 `FINALIZED` 데이터를 조회하도록 유지한다.
- [x] `StaffArcControllerTest`와 `CustomerArcControllerTest`가 새 생성자 시그니처와 공개 API 목록에 맞게 컴파일되고 통과하도록 수정한다.

### Task 4: 상태 전이 문서와 회귀 검증

**Files:** `docs/adr/ADR-003-Visit과-Arc-매칭-상태-전이.md`, all affected tests.

- [x] ADR의 Arc 흐름을 `READY → SHARED` 즉시 공개와 SHARED 리비전 교체 방식으로 갱신한다.
- [x] ADR에서 고객 Arc 최종 저장을 Arc 완료 조건으로 설명하는 문장을 제거하고, 최초 Arc 생성 성공 시 Visit이 COMPLETED가 되는 결정으로 갱신한다.
- [x] 직원 `다시 생성하기`는 기존 입력 재사용, `수정하기`는 수정된 inputSnapshot 전달이라는 API 계약을 문서화한다.
- [x] Arc 생성·재생성·실패·고객 목록 조회의 관련 테스트를 실행한다.
- [x] `./gradlew test --stacktrace --no-daemon`을 실행해 전체 회귀를 검증한다.
- [x] `git diff --check`를 실행하고 변경 파일이 이슈 범위에 한정되는지 확인한다.
