# Visit Memory 공유와 고객 방문 종료 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 직원 Visit Memory 생성·재생성·공유, 고객 웹 알림·상세 조회와 고객 Arc 최종 저장을 구현한다.

**Architecture:** 기존 Arc 흐름을 따라 외부 OpenAI 호출 조정 서비스와 트랜잭션 상태 서비스를 분리한다. Visit Memory는 단일 초안을 갱신하고 공유 트랜잭션에서 방문 완료와 고객 알림을 함께 저장한다. 고객 API는 익명 쿠키 토큰으로 소유권을 검증한다.

**Tech Stack:** Java 21, Spring Boot 4.1.0, Spring MVC, Spring Data JPA, Flyway, PostgreSQL/MySQL, JUnit 5, Mockito, MockMvc.

## Global Constraints

- 새 Java 소스 파일 첫 줄에는 역할을 설명하는 한 줄짜리 한국어 주석을 둔다.
- 적용된 Flyway migration은 수정하지 않고 `V5`를 추가한다.
- 공개 API에는 Springdoc `@Operation`과 HTTP 회귀 테스트를 함께 추가한다.
- OpenAI 호출은 트랜잭션 밖에서 수행한다.
- 최종 검증은 `./gradlew test --stacktrace --no-daemon`과 `./gradlew spotlessCheck --no-daemon`으로 수행한다.

---

### Task 1: Visit Memory 상태 모델

**Files:**
- Modify: `src/main/java/com/lionthanflower/domain/visitmemory/entity/VisitMemoryStatus.java`
- Modify: `src/main/java/com/lionthanflower/domain/visitmemory/entity/VisitMemory.java`
- Create: `src/main/java/com/lionthanflower/domain/visitmemory/entity/VisitMemoryGeneratedContent.java`
- Test: `src/test/java/com/lionthanflower/domain/visitmemory/entity/VisitMemoryTest.java`

**Interfaces:**
- Produces: `VisitMemoryGeneratedContent(String summary)`.
- Produces: `VisitMemory.replaceInput(VisitMemoryInputSnapshot)`, `completeGeneration(String, Instant)`, `finalizeMemory(Instant)`.

- [x] **Step 1: 상태 전이 실패 테스트를 작성한다.**

```java
memory.startGeneration();
memory.completeGeneration("{\"summary\":\"방문 기록\"}", generatedAt);
assertThat(memory.getStatus()).isEqualTo(VisitMemoryStatus.READY);
memory.finalizeMemory(finalizedAt);
assertThat(memory.getStatus()).isEqualTo(VisitMemoryStatus.FINALIZED);
```

- [x] **Step 2: 도메인 테스트 실패를 확인한다.**

Run: `./gradlew test --tests '*VisitMemoryTest' --no-daemon`
Expected: `READY`, `completeGeneration` 또는 `finalizeMemory` 부재로 FAIL.

- [x] **Step 3: 단일 초안 상태 전이와 입력 교체를 최소 구현한다.**

```java
public enum VisitMemoryStatus { DRAFT, GENERATING, READY, FINALIZED, FAILED }

public void replaceInput(VisitMemoryInputSnapshot inputSnapshot) {
  if (status != VisitMemoryStatus.READY && status != VisitMemoryStatus.FAILED) {
    throw new IllegalStateException("생성 완료 또는 실패 상태에서만 입력을 수정할 수 있습니다.");
  }
  this.inputSnapshot = SnapshotJsonSerializer.serialize(inputSnapshot);
}
```

- [x] **Step 4: 도메인 테스트 통과를 확인한다.**

Run: `./gradlew test --tests '*VisitMemoryTest' --no-daemon`
Expected: PASS.

### Task 2: 고객 알림 영속성

**Files:**
- Create: `src/main/java/com/lionthanflower/domain/notification/entity/CustomerNotification.java`
- Create: `src/main/java/com/lionthanflower/domain/notification/entity/CustomerNotificationType.java`
- Create: `src/main/java/com/lionthanflower/infrastructure/persistence/CustomerNotificationRepository.java`
- Create: `src/main/resources/db/migration/V5__create_customer_notifications.sql`
- Test: `src/test/java/com/lionthanflower/domain/notification/entity/CustomerNotificationTest.java`
- Modify: `src/test/java/com/lionthanflower/infrastructure/persistence/InitialDomainPersistenceTest.java`
- Modify: `src/test/java/com/lionthanflower/infrastructure/persistence/InitialDomainMySqlMigrationTest.java`

**Interfaces:**
- Produces: `CustomerNotification.createVisitMemory(UUID customerId, UUID visitMemoryId, String message)`.
- Produces: `markRead(Instant readAt)` and `isRead()`.
- Produces: repository latest-first customer lookup and customer-scoped ID lookup.

- [x] **Step 1: PostgreSQL/MySQL migration과 엔티티 상태 테스트를 작성한다.**
- [x] **Step 2: 관련 테스트 실행을 시도한다.**

Run: `./gradlew test --tests '*InitialDomain*MigrationTest' --no-daemon`
Expected: `customer_notifications` 부재로 FAIL.

- [x] **Step 3: V5 migration과 알림 엔티티·저장소를 구현한다.**

```sql
CREATE TABLE customer_notifications (
    id CHAR(36) NOT NULL,
    customer_id CHAR(36) NOT NULL,
    type VARCHAR(40) NOT NULL,
    resource_id CHAR(36) NOT NULL,
    message VARCHAR(255) NOT NULL,
    read_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT pk_customer_notifications PRIMARY KEY (id),
    CONSTRAINT uk_customer_notifications_type_resource UNIQUE (customer_id, type, resource_id),
    CONSTRAINT fk_customer_notifications_customer FOREIGN KEY (customer_id) REFERENCES customers (id)
);
```

- [ ] **Step 4: migration과 알림 테스트 통과를 확인한다.**

Docker Desktop이 실행되지 않아 Testcontainers 기반 PostgreSQL/MySQL 검증은 보류한다.

Run: `./gradlew test --tests '*InitialDomain*MigrationTest' --no-daemon`
Expected: PASS.

### Task 3: Visit Memory OpenAI Port와 어댑터

**Files:**
- Create: `src/main/java/com/lionthanflower/application/visitmemory/VisitMemoryGenerationCommand.java`
- Create: `src/main/java/com/lionthanflower/application/visitmemory/VisitMemoryGenerationPort.java`
- Create: `src/main/java/com/lionthanflower/infrastructure/openai/OpenAiVisitMemoryGenerationClient.java`
- Create: `src/test/java/com/lionthanflower/infrastructure/openai/OpenAiVisitMemoryGenerationClientTest.java`
- Modify: `src/main/resources/application.yml`

**Interfaces:**
- Produces: `VisitMemoryGenerationPort.generate(VisitMemoryGenerationCommand)` returning `VisitMemoryGeneratedContent`.
- Command fields: customer name, additional request and `VisitMemoryInputSnapshot`.

- [x] **Step 1: Responses API 요청 스키마와 응답 파싱 실패 테스트를 작성한다.**
- [x] **Step 2: OpenAI 어댑터 테스트 실패를 확인한다.**

Run: `./gradlew test --tests '*OpenAiVisitMemoryGenerationClientTest' --no-daemon`
Expected: 구현 클래스 부재로 FAIL.

- [x] **Step 3: Arc 어댑터와 같은 오류 경계로 summary JSON 생성을 구현한다.**

```java
public interface VisitMemoryGenerationPort {
  VisitMemoryGeneratedContent generate(VisitMemoryGenerationCommand command);
}
```

- [x] **Step 4: OpenAI 어댑터 테스트 통과를 확인한다.**

Run: `./gradlew test --tests '*OpenAiVisitMemoryGenerationClientTest' --no-daemon`
Expected: PASS.

### Task 4: 직원 Visit Memory 상태·오케스트레이션 서비스

**Files:**
- Create: `src/main/java/com/lionthanflower/infrastructure/persistence/VisitMemoryRepository.java`
- Create: `src/main/java/com/lionthanflower/application/staff/dto/StaffVisitMemoryGenerationRequest.java`
- Create: `src/main/java/com/lionthanflower/application/staff/dto/StaffVisitMemoryResponse.java`
- Create: `src/main/java/com/lionthanflower/domain/visitmemory/error/VisitMemoryErrorCode.java`
- Create: `src/main/java/com/lionthanflower/application/staff/StaffVisitMemoryStateService.java`
- Create: `src/main/java/com/lionthanflower/application/staff/StaffVisitMemoryService.java`
- Test: `src/test/java/com/lionthanflower/application/staff/StaffVisitMemoryStateServiceTest.java`
- Test: `src/test/java/com/lionthanflower/application/staff/StaffVisitMemoryServiceTest.java`

**Interfaces:**
- Produces: `create(UUID visitId, Staff, StaffVisitMemoryGenerationRequest)`.
- Produces: `regenerate(UUID memoryId, Staff, StaffVisitMemoryGenerationRequest)` with null input meaning existing input.
- Produces: `getPreview(UUID memoryId, Staff)` and `share(UUID memoryId, Staff)`.

- [x] **Step 1: Solo 자동 배정, 중복 생성, 기존·수정 입력 재생성, 성공·실패와 공유 트랜잭션 테스트를 작성한다.**
- [x] **Step 2: 서비스 테스트 실패를 확인한다.**

Run: `./gradlew test --tests '*StaffVisitMemory*Test' --no-daemon`
Expected: 서비스와 DTO 부재로 FAIL.

- [x] **Step 3: Arc 서비스 패턴으로 상태 서비스와 외부 호출 조정 서비스를 구현한다.**

```java
public record GenerationContext(UUID visitMemoryId, VisitMemoryGenerationCommand command) {}

private StaffVisitMemoryResponse generate(GenerationContext context) {
  try {
    return stateService.complete(context.visitMemoryId(), generationPort.generate(context.command()));
  } catch (RuntimeException exception) {
    return stateService.fail(context.visitMemoryId(), "OPENAI_GENERATION_FAILED");
  }
}
```

- [x] **Step 4: 직원 Visit Memory 서비스 테스트 통과를 확인한다.**

Run: `./gradlew test --tests '*StaffVisitMemory*Test' --no-daemon`
Expected: PASS.

### Task 5: 직원 Visit Memory HTTP API

**Files:**
- Create: `src/main/java/com/lionthanflower/controller/staff/StaffVisitMemoryController.java`
- Create: `src/test/java/com/lionthanflower/controller/staff/StaffVisitMemoryControllerTest.java`

**Interfaces:**
- Produces the four issue #29 staff endpoints with `ApiResponse<StaffVisitMemoryResponse>`.

- [x] **Step 1: 생성·미리보기·재생성·공유와 미인증 MockMvc 테스트를 작성한다.**
- [x] **Step 2: 컨트롤러 테스트 실패를 확인한다.**

Run: `./gradlew test --tests '*StaffVisitMemoryControllerTest' --no-daemon`
Expected: endpoint 부재로 FAIL.

- [x] **Step 3: Arc 컨트롤러와 같은 인증·Springdoc 패턴으로 컨트롤러를 구현한다.**
- [x] **Step 4: 컨트롤러 테스트 통과를 확인한다.**

Run: `./gradlew test --tests '*StaffVisitMemoryControllerTest' --no-daemon`
Expected: PASS.

### Task 6: 고객 알림과 Visit Memory 상세 API

**Files:**
- Create: `src/main/java/com/lionthanflower/application/customer/CustomerNotificationService.java`
- Create: `src/main/java/com/lionthanflower/application/customer/CustomerVisitMemoryQueryService.java`
- Create: `src/main/java/com/lionthanflower/infrastructure/web/customer/CustomerNotificationController.java`
- Create: `src/main/java/com/lionthanflower/infrastructure/web/customer/CustomerVisitMemoryController.java`
- Test: `src/test/java/com/lionthanflower/application/customer/CustomerNotificationServiceTest.java`
- Test: `src/test/java/com/lionthanflower/application/customer/CustomerVisitMemoryQueryServiceTest.java`
- Test: `src/test/java/com/lionthanflower/infrastructure/web/customer/CustomerNotificationControllerTest.java`
- Test: `src/test/java/com/lionthanflower/infrastructure/web/customer/CustomerVisitMemoryControllerTest.java`

**Interfaces:**
- Produces: latest-first notification list and idempotent `markRead`.
- Produces: finalized owner-only Visit Memory detail with store name/country, summary and finalizedAt.

- [x] **Step 1: 인증, 소유권, 최신순, 멱등 읽음과 비공개 초안 404 테스트를 작성한다.**
- [x] **Step 2: 고객 API 테스트 실패를 확인한다.**

Run: `./gradlew test --tests '*CustomerNotification*Test' --tests '*CustomerVisitMemory*Test' --no-daemon`
Expected: 서비스와 endpoint 부재로 FAIL.

- [x] **Step 3: 고객 토큰 검증과 404 공개 경계를 적용해 서비스·컨트롤러를 구현한다.**
- [x] **Step 4: 고객 알림·상세 테스트 통과를 확인한다.**

Run: `./gradlew test --tests '*CustomerNotification*Test' --tests '*CustomerVisitMemory*Test' --no-daemon`
Expected: PASS.

### Task 7: 고객 Arc 최종 저장

**Files:**
- Create: `src/main/java/com/lionthanflower/application/customer/CustomerArcCommandService.java`
- Modify: `src/main/java/com/lionthanflower/infrastructure/web/customer/CustomerArcController.java`
- Create: `src/test/java/com/lionthanflower/application/customer/CustomerArcCommandServiceTest.java`
- Modify: `src/test/java/com/lionthanflower/infrastructure/web/customer/CustomerArcControllerTest.java`

**Interfaces:**
- Produces: `finalizeArc(UUID arcId, String rawToken)` returning arc/visit finalization response.

- [x] **Step 1: SHARED 정상 완료, 다른 고객 404, DRAFT 409와 FINALIZED 멱등 테스트를 작성한다.**
- [x] **Step 2: Arc command 테스트 실패를 확인한다.**

Run: `./gradlew test --tests '*CustomerArcCommandServiceTest' --tests '*CustomerArcControllerTest' --no-daemon`
Expected: finalize 서비스와 endpoint 부재로 FAIL.

- [x] **Step 3: Arc 최종화와 Visit 완료를 한 트랜잭션으로 구현한다.**

```java
if (arc.getStatus() == ArcStatus.FINALIZED) {
  return response(arc, visit);
}
if (arc.getStatus() != ArcStatus.SHARED) {
  throw new BusinessException(ArcErrorCode.NOT_ASSIGNABLE);
}
arc.finalizeSharedRevision(now);
visit.complete(now);
```

- [x] **Step 4: 고객 Arc 최종 저장 테스트 통과를 확인한다.**

Run: `./gradlew test --tests '*CustomerArcCommandServiceTest' --tests '*CustomerArcControllerTest' --no-daemon`
Expected: PASS.

### Task 8: ADR과 전체 검증

**Files:**
- Modify: `docs/adr/ADR-003-Visit과-Arc-매칭-상태-전이.md`

- [x] **Step 1: READY·직원 공유·고객 웹 알림과 단일 초안 결정을 ADR에 반영한다.**
- [x] **Step 2: Spotless를 적용하고 검사한다.**

Run: `./gradlew spotlessApply spotlessCheck --no-daemon`
Expected: PASS.

- [ ] **Step 3: 전체 테스트를 실행한다.**

전체 테스트는 Docker Desktop 미실행으로 Testcontainers 기반 13개 테스트가 실패했고, 추가한 비통합 테스트는 별도 전체 통과했다.

Run: `./gradlew test --stacktrace --no-daemon`
Expected: PASS.

- [x] **Step 4: diff 오류와 작업 범위를 확인한다.**

Run: `git diff --check`
Expected: 출력 없음.
