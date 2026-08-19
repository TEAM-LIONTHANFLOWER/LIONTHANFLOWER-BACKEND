# Hotfix 36 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 고객·직원 웹 연동을 막는 방문 생성, 언어, 매장 검색, 매칭 조회, CORS와 쿠키 문제를 해결한다.

**Architecture:** 기존 Spring MVC 계층과 JPA 저장소를 유지한다. 데이터 누락은 Flyway로 보정하고, 조회 API는 Application Service를 거치며, CORS는 Spring Security의 단일 `CorsConfigurationSource`에서 환경별 Origin을 적용한다.

**Tech Stack:** Java 21, Spring Boot 4.1.0, Spring Web MVC, Spring Security, Spring Data JPA, Flyway, JUnit 5, MockMvc, Testcontainers.

## Global Constraints

- 기준 브랜치는 `main`, 작업 브랜치는 `hotfix/36`이다.
- 새 소스 파일 첫 줄에는 역할을 설명하는 한 줄짜리 한국어 주석을 둔다.
- 적용된 Flyway migration은 수정하지 않고 다음 번호인 V6를 추가한다.
- 공개 API에는 Springdoc `@Operation`과 HTTP 테스트를 함께 추가한다.
- 사용자 변경인 `docs/tasks/34/design.md`는 수정하거나 커밋하지 않는다.
- 완료 전 `./gradlew test --stacktrace --no-daemon`을 실행한다.

---

### Task 1: 기본 매장과 한국어 코드

**Files:**
- Create: `src/main/resources/db/migration/V6__seed_mcm_seoul_store.sql`
- Modify: `src/main/java/com/lionthanflower/domain/common/entity/LanguageCode.java`
- Modify: `src/test/java/com/lionthanflower/infrastructure/persistence/InitialDomainPersistenceTest.java`
- Modify: `src/test/java/com/lionthanflower/infrastructure/persistence/InitialDomainMySqlMigrationTest.java`
- Modify: `src/test/java/com/lionthanflower/infrastructure/web/customer/CustomerVisitControllerTest.java`

**Interfaces:**
- Produces: 코드가 `MCM-SEOUL`인 매장 한 건과 `LanguageCode.KO`.

- [x] **Step 1: PostgreSQL과 MySQL migration 테스트에 기본 매장 조회 검증을 추가한다.**

```java
@Test
void MCM_서울_기본_매장이_생성된다() throws SQLException {
  assertThat(countStoresByCode("MCM-SEOUL")).isEqualTo(1);
}
```

- [x] **Step 2: 고객 온보딩 HTTP 테스트에 `serviceLanguage: KO` 요청을 추가한다.**

```java
mockMvc.perform(patch("/api/customers/visits/{visitId}/onboarding", visitId)
    .cookie(new Cookie("customer_token", "known-token"))
    .contentType(MediaType.APPLICATION_JSON)
    .content("""{"name":"홍길동","serviceLanguage":"KO","interactionStyle":"SELF_GUIDED"}"""))
    .andExpect(status().isOk());
```

- [x] **Step 3: 관련 테스트를 실행해 매장 0건과 `KO` 역직렬화 실패를 확인한다.**

Run: `./gradlew test --tests '*InitialDomainPersistenceTest.MCM*' --tests '*InitialDomainMySqlMigrationTest.MCM*' --tests '*CustomerVisitControllerTest.*KO*' --stacktrace --no-daemon`

- [x] **Step 4: V6 migration과 `LanguageCode.KO`를 최소 구현한다.**

```sql
-- 고객 방문 생성에 필요한 MCM 서울 기본 매장을 초기화하는 마이그레이션
INSERT INTO stores (id, name, code, country_code, created_at, updated_at)
SELECT '00000000-0000-0000-0000-000000000001', 'MCM Seoul', 'MCM-SEOUL', 'KR', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE code = 'MCM-SEOUL');
```

```java
public enum LanguageCode {
  KO,
  EN,
  ZH,
  JA,
  RU
}
```

- [x] **Step 5: 관련 테스트를 다시 실행해 통과를 확인한다.**

- [x] **Step 6: `36 fix: 기본 매장과 한국어 언어 코드 추가`로 커밋한다.**

### Task 2: 공개 매장 검색 API

**Files:**
- Create: `src/main/java/com/lionthanflower/application/store/StoreQueryService.java`
- Create: `src/main/java/com/lionthanflower/infrastructure/web/store/StoreController.java`
- Create: `src/test/java/com/lionthanflower/application/store/StoreQueryServiceTest.java`
- Create: `src/test/java/com/lionthanflower/infrastructure/web/store/StoreControllerTest.java`
- Modify: `src/main/java/com/lionthanflower/infrastructure/persistence/StoreRepository.java`
- Modify: `src/main/java/com/lionthanflower/global/config/CustomerApiSecurityConfig.java`

**Interfaces:**
- Produces: `StoreQueryService.search(String query): List<StoreSummary>`.
- Produces: 공개 `GET /api/stores?query={query}` 응답의 `storeId`, `name`, `code`, `countryCode`.

- [x] **Step 1: 이름 또는 코드 검색과 최대 20건 정렬을 검증하는 Service 테스트를 작성한다.**

```java
when(storeRepository.findTop20ByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByNameAsc("seoul", "seoul"))
    .thenReturn(List.of(Store.create("MCM Seoul", "MCM-SEOUL", "KR")));
assertThat(service.search(" seoul ")).extracting(StoreSummary::code).containsExactly("MCM-SEOUL");
```

- [x] **Step 2: 미인증 공개 GET 응답과 OpenAPI 설명을 검증하는 Controller 테스트를 작성한다.**

- [x] **Step 3: 새 테스트를 실행해 컴파일 또는 Bean 부재로 실패하는지 확인한다.**

Run: `./gradlew test --tests '*StoreQueryServiceTest' --tests '*StoreControllerTest' --stacktrace --no-daemon`

- [x] **Step 4: Repository 파생 쿼리, `StoreQueryService`, `StoreController`와 `/api/stores` 보안 허용을 구현한다.**

```java
List<Store> findTop20ByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByNameAsc(
    String name, String code);
```

```java
@GetMapping("/api/stores")
public ApiResponse<List<StoreResponse>> search(@RequestParam(defaultValue = "") String query)
```

- [x] **Step 5: 관련 테스트를 다시 실행해 통과를 확인한다.**

- [x] **Step 6: `36 feat: 공개 매장 검색 API 추가`로 커밋한다.**

### Task 3: 고객 매칭 상태 조회 API

**Files:**
- Modify: `src/main/java/com/lionthanflower/application/customer/CustomerVisitService.java`
- Modify: `src/main/java/com/lionthanflower/infrastructure/web/customer/CustomerVisitController.java`
- Modify: `src/test/java/com/lionthanflower/application/customer/CustomerVisitServiceTest.java`
- Modify: `src/test/java/com/lionthanflower/infrastructure/web/customer/CustomerVisitControllerTest.java`

**Interfaces:**
- Produces: `CustomerVisitService.getMatching(UUID visitId, String rawToken): MatchingResult`.
- Produces: `GET /api/customers/visits/{visitId}/matching` 응답의 `visitId`, `status`, `staffId`, `staffName`, `matchedAt`.

- [x] **Step 1: 소유 고객의 대기 상태와 직원 배정 완료 상태를 검증하는 Service 테스트를 작성한다.**

```java
MatchingResult result = service.getMatching(visit.getId(), "known-token");
assertThat(result.status()).isEqualTo(VisitStatus.ACTIVE);
assertThat(result.staffName()).isEqualTo("김형진");
assertThat(result.matchedAt()).isNotNull();
```

- [x] **Step 2: 토큰 누락과 다른 고객의 방문이 각각 400과 404가 되는 Service 테스트를 작성한다.**

- [x] **Step 3: 고객 쿠키로 호출하는 GET API 응답과 `@Operation`을 검증하는 Controller 테스트를 작성한다.**

- [x] **Step 4: 관련 테스트를 실행해 메서드 부재로 실패하는지 확인한다.**

Run: `./gradlew test --tests '*CustomerVisitServiceTest' --tests '*CustomerVisitControllerTest' --stacktrace --no-daemon`

- [x] **Step 5: 기존 고객 토큰 및 방문 소유권 확인 로직을 재사용해 Service와 Controller를 구현한다.**

```java
public record MatchingResult(
    UUID visitId, VisitStatus status, UUID staffId, String staffName, Instant matchedAt) {}
```

- [x] **Step 6: 관련 테스트를 다시 실행해 통과를 확인한다.**

- [x] **Step 7: `36 feat: 고객 매칭 상태 조회 API 추가`로 커밋한다.**

### Task 4: 환경별 CORS와 교차 사이트 쿠키

**Files:**
- Modify: `src/main/java/com/lionthanflower/global/config/CustomerApiSecurityConfig.java`
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/resources/application-dev.yml`
- Modify: `src/main/resources/application-prod.yml`
- Modify: `src/main/java/com/lionthanflower/infrastructure/web/customer/CustomerVisitController.java`
- Modify: `src/main/java/com/lionthanflower/controller/staff/StaffProfileController.java`
- Modify: `src/test/java/com/lionthanflower/global/config/CustomerApiSecurityConfigTest.java`
- Modify: `src/test/java/com/lionthanflower/infrastructure/web/customer/CustomerVisitControllerTest.java`
- Modify: `src/test/java/com/lionthanflower/controller/staff/StaffProfileControllerTest.java`

**Interfaces:**
- Consumes: `app.cors.allowed-origins`, `app.customer-session.cookie-same-site`, `app.staff-session.cookie-same-site`.
- Produces: `/api/**` CORS 응답과 환경별 `SameSite` 쿠키.

- [x] **Step 1: 허용 Origin의 GET과 OPTIONS 응답 헤더, 비허용 Origin 차단을 Security 테스트에 작성한다.**

```java
mockMvc.perform(options("/api/test")
    .header(HttpHeaders.ORIGIN, "http://localhost:8081")
    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
    .andExpect(status().isOk())
    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8081"))
    .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
```

- [x] **Step 2: 고객과 직원 쿠키 테스트의 기대값을 `SameSite=None`으로 변경한다.**

- [x] **Step 3: 관련 테스트를 실행해 CORS 헤더 부재와 쿠키 기대값 불일치를 확인한다.**

Run: `./gradlew test --tests '*CustomerApiSecurityConfigTest' --tests '*CustomerVisitControllerTest' --tests '*StaffProfileControllerTest' --stacktrace --no-daemon`

- [x] **Step 4: `CorsConfigurationSource`를 SecurityFilterChain에 연결하고 `/api/**`에 credentials, Origin, 메서드 정책을 등록한다.**

```java
configuration.setAllowedOrigins(allowedOrigins);
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
configuration.setAllowedHeaders(List.of("*"));
configuration.setAllowCredentials(true);
source.registerCorsConfiguration("/api/**", configuration);
```

- [x] **Step 5: local 기본 Origin, dev Pages Origin, prod Pages Origin과 dev/prod `SameSite=None; Secure` 설정을 추가한다.**

- [x] **Step 6: Controller가 설정된 SameSite 값을 사용하도록 최소 수정한다.**

- [x] **Step 7: 관련 테스트를 다시 실행해 통과를 확인한다.**

- [x] **Step 8: `36 fix: 웹 CORS와 교차 사이트 쿠키 설정`으로 커밋한다.**

### Task 5: 전체 회귀 검증

**Files:**
- Review: 이슈 #36에서 수정한 모든 파일.

**Interfaces:**
- Produces: 전체 테스트 통과와 범위가 제한된 최종 diff.

- [x] **Step 1: Spotless를 실행해 프로젝트 포맷을 적용한다.**

Run: `./gradlew spotlessApply --no-daemon`

- [x] **Step 2: 전체 테스트를 실행한다.**

Run: `./gradlew test --stacktrace --no-daemon`

- [x] **Step 3: diff whitespace와 변경 범위를 검증한다.**

Run: `git diff --check && git status --short && git diff main...HEAD --stat`

- [x] **Step 4: 포맷으로 인한 미커밋 변경이 있으면 해당 논리 커밋에 포함하고 관련 테스트를 재실행한다.**

- [x] **Step 5: `docs/tasks/34/design.md`가 어떤 커밋에도 포함되지 않았는지 확인한다.**
