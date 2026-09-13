# 파리·뮌헨 매장 선택 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 파리·뮌헨 매장을 시드하고 고객 방문 생성 시 선택한 매장을 연결하여 Arc가 올바른 도시 코드를 반환하게 한다.

**Architecture:** 기존 `POST /api/customers/visits`에 선택적 `storeCode` 쿼리 파라미터를 추가한다. Application Service가 명시된 코드 또는 기존 기본 설정 코드로 Store를 조회한 뒤 그 ID로 Visit을 생성하며, Arc 조회의 기존 `Arc → Visit → Store` 경로는 변경하지 않는다.

**Tech Stack:** Java 21, Spring Boot 4.1.0, Spring Web MVC, Spring Data JPA, Flyway, PostgreSQL 17, MySQL 8.4, JUnit 5, Mockito, MockMvc, Testcontainers.

**Spec:** `docs/tasks/62/design.md`

## Global Constraints

- 적용된 V8 마이그레이션은 수정하지 않고 다음 버전인 V9를 추가한다.
- 파리는 `MCM-PARIS / FR / PARIS`, 뮌헨은 `MCM-MUNICH / DE / MUNICH`로 저장한다.
- `storeCode` 생략은 기존 기본 매장 설정을 사용하고 기존 응답 및 쿠키 계약을 유지한다.
- 명시된 빈 코드는 `COMMON-400`, 존재하지 않는 코드는 `COMMON-404`, 누락된 기본 매장은 `COMMON-500`으로 처리한다.
- 기존 Arc 조회 구현은 변경하지 않고 선택된 `Visit.storeId`를 통해 도시가 연결되는지 검증한다.
- 새 Java 또는 SQL 파일의 첫 줄에는 파일 역할을 설명하는 한 줄짜리 한국어 주석을 둔다.

---

### Task 1: 파리·뮌헨 매장 Flyway 시드

**Files:**
- Create: `src/main/resources/db/migration/V9__seed_paris_and_munich_stores.sql`
- Modify: `src/test/java/com/lionthanflower/infrastructure/persistence/StoreCityMigrationTest.java`

**Interfaces:**
- Consumes: V8의 `stores.city_code VARCHAR(100)`와 기존 `stores.code` 고유 제약.
- Produces: 코드 `MCM-PARIS`, `MCM-MUNICH`로 조회 가능한 고정 Store 행.

- [x] **Step 1: V9가 두 매장을 추가하는 실패 테스트 작성**

`StoreCityMigrationTest`에 PostgreSQL과 MySQL을 모두 실행하는 테스트를 추가한다. V8까지만 적용한 DB에 V9를 적용하고 다음 값을 JDBC로 조회한다.

```java
@ParameterizedTest
@ValueSource(strings = {"postgres", "mysql"})
void 파리와_뮌헨_매장을_고정_도시_코드로_추가한다(String databaseType) throws Exception {
  try (JdbcDatabaseContainer<?> database = createDatabase(databaseType)) {
    database.start();
    var configuration =
        Flyway.configure()
            .dataSource(database.getJdbcUrl(), database.getUsername(), database.getPassword())
            .locations("classpath:db/migration");
    configuration.target("8").load().migrate();

    var flyway = configuration.target("9").load();
    assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
    assertStore(database, "MCM-PARIS", "MCM Paris", "FR", "PARIS");
    assertStore(database, "MCM-MUNICH", "MCM Munich", "DE", "MUNICH");
    assertThat(flyway.migrate().migrationsExecuted).isZero();
  }
}
```

같은 파일에 `createDatabase(String)`와 `assertStore(...)` private helper를 두어 기존 중복 컨테이너·JDBC 코드를 줄인다. 기존 V8 테스트도 `createDatabase`를 사용하도록 필요한 줄만 변경한다.

- [x] **Step 2: V9 부재로 테스트가 실패하는지 확인**

Run:

```bash
./gradlew test --tests '*StoreCityMigrationTest' --stacktrace --no-daemon
```

Expected: V9 적용 건수가 0이거나 `MCM-PARIS` 조회 결과가 없어 실패한다.

- [x] **Step 3: 동일 코드 보존 테스트 작성**

V8 상태에서 `MCM-PARIS` 코드를 가진 기존 행을 다른 UUID와 `city_code = LEGACY_PARIS`로 삽입한 뒤 V9를 적용하는 테스트를 추가한다. 적용 후 해당 행이 한 건이고 UUID와 `LEGACY_PARIS`가 유지되며 뮌헨 행은 정상 생성되어야 한다.

```java
assertThat(countStoresByCode(database, "MCM-PARIS")).isEqualTo(1);
assertStore(database, "MCM-PARIS", "Legacy Paris", "FR", "LEGACY_PARIS");
assertStore(database, "MCM-MUNICH", "MCM Munich", "DE", "MUNICH");
```

- [x] **Step 4: 최소 V9 마이그레이션 구현**

`V9__seed_paris_and_munich_stores.sql`을 다음 형태로 작성한다.

```sql
-- 고객 방문 매장 선택에 사용할 파리와 뮌헨 매장을 초기화합니다.
INSERT INTO stores (id, name, code, country_code, city_code, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000002',
    'MCM Paris',
    'MCM-PARIS',
    'FR',
    'PARIS',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE code = 'MCM-PARIS');

INSERT INTO stores (id, name, code, country_code, city_code, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000003',
    'MCM Munich',
    'MCM-MUNICH',
    'DE',
    'MUNICH',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM stores WHERE code = 'MCM-MUNICH');
```

- [x] **Step 5: 마이그레이션 테스트 통과 확인**

Run:

```bash
./gradlew test --tests '*StoreCityMigrationTest' --stacktrace --no-daemon
```

Expected: PostgreSQL·MySQL의 신규 생성, 동일 코드 보존, V8 회귀 테스트가 모두 PASS.

- [x] **Step 6: 첫 논리 변경 커밋**

```bash
git add src/main/resources/db/migration/V9__seed_paris_and_munich_stores.sql src/test/java/com/lionthanflower/infrastructure/persistence/StoreCityMigrationTest.java
git commit -m "62 feat: 파리와 뮌헨 매장 데이터 추가"
```

---

### Task 2: 고객 방문의 매장 선택 API

**Files:**
- Modify: `src/main/java/com/lionthanflower/application/customer/CustomerVisitService.java`
- Modify: `src/main/java/com/lionthanflower/infrastructure/web/customer/CustomerVisitController.java`
- Modify: `src/test/java/com/lionthanflower/application/customer/CustomerVisitServiceTest.java`
- Modify: `src/test/java/com/lionthanflower/infrastructure/web/customer/CustomerVisitControllerTest.java`

**Interfaces:**
- Consumes: `StoreRepository.findByCode(String)`와 Task 1의 `MCM-SEOUL`, `MCM-PARIS`, `MCM-MUNICH` 매장 코드.
- Produces: `CustomerVisitService.enter(String rawToken, String requestedStoreCode)`와 `POST /api/customers/visits?storeCode=...`.

- [x] **Step 1: Service 매장 선택 실패 테스트 작성**

기존 `service.enter(rawToken)` 호출은 모두 `service.enter(rawToken, null)`로 바꾼다. 이어서 다음 세 테스트를 추가한다.

```java
@Test
void 명시한_매장으로_방문을_생성한다() {
  Store paris = Store.create("MCM Paris", "MCM-PARIS", "FR", "PARIS");
  when(storeRepository.findByCode("MCM-PARIS")).thenReturn(Optional.of(paris));
  when(customerRepository.save(any(Customer.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));
  when(visitRepository.save(any(Visit.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));

  service.enter(null, "MCM-PARIS");

  var visit = ArgumentCaptor.forClass(Visit.class);
  verify(visitRepository).save(visit.capture());
  assertThat(visit.getValue().getStoreId()).isEqualTo(paris.getId());
}

@Test
void 비어_있는_명시적_매장_코드는_잘못된_요청이다() {
  assertThatThrownBy(() -> service.enter(null, " "))
      .isInstanceOf(BusinessException.class)
      .extracting(exception -> ((BusinessException) exception).errorCode())
      .isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE);
}

@Test
void 존재하지_않는_명시적_매장_코드는_찾을_수_없다() {
  when(storeRepository.findByCode("MCM-UNKNOWN")).thenReturn(Optional.empty());

  assertThatThrownBy(() -> service.enter(null, "MCM-UNKNOWN"))
      .isInstanceOf(BusinessException.class)
      .extracting(exception -> ((BusinessException) exception).errorCode())
      .isEqualTo(CommonErrorCode.NOT_FOUND);
}
```

기존 `설정된_매장이_없으면_서버_오류를_반환한다` 테스트는 `service.enter(null, null)`로 호출하여 `COMMON-500` 계약을 유지한다.

- [x] **Step 2: Controller 전달 실패 테스트 작성**

기존 mock을 `service.enter(rawToken, null)` 시그니처로 바꾼다. 다음 테스트에서 HTTP 쿼리 파라미터가 그대로 Service에 전달되는지 확인한다.

```java
@Test
void 고객은_매장_코드를_선택해_서비스에_진입한다() throws Exception {
  UUID visitId = UUID.randomUUID();
  when(service.enter(null, "MCM-MUNICH"))
      .thenReturn(
          new CustomerVisitService.EntryResult(
              visitId, null, VisitStatus.ONBOARDING, "issued-token"));

  mockMvc
      .perform(post("/api/customers/visits").param("storeCode", "MCM-MUNICH"))
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.data.visitId").value(visitId.toString()));

  verify(service).enter(null, "MCM-MUNICH");
}
```

- [x] **Step 3: 새 시그니처 부재로 테스트가 실패하는지 확인**

Run:

```bash
./gradlew test --tests '*CustomerVisitServiceTest' --tests '*CustomerVisitControllerTest' --stacktrace --no-daemon
```

Expected: `enter(String, String)` 메서드가 없어 테스트 컴파일이 실패한다.

- [x] **Step 4: Service에 최소 매장 해석 로직 구현**

공개 메서드를 다음 시그니처로 바꾸고 Store 조회를 helper로 분리한다.

```java
public EntryResult enter(String rawToken, String requestedStoreCode) {
  Store store = resolveEntryStore(requestedStoreCode);
  CustomerSession session = resolveCustomer(rawToken);
  Visit visit = visitRepository.save(Visit.create(session.customer().getId(), store.getId()));
  return new EntryResult(
      visit.getId(), session.customer().getName(), visit.getStatus(), session.issuedToken());
}

private Store resolveEntryStore(String requestedStoreCode) {
  if (requestedStoreCode == null) {
    return storeRepository
        .findByCode(storeCode)
        .orElseThrow(() -> new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR));
  }
  if (requestedStoreCode.isBlank()) {
    throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
  }
  return storeRepository
      .findByCode(requestedStoreCode)
      .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
}
```

명시적 코드의 대소문자 변환이나 trim은 하지 않는다. API 계약인 고유 매장 코드와 정확히 일치시킨다.

- [x] **Step 5: Controller와 Springdoc 계약 구현**

`enter`에 선택적 쿼리 파라미터를 추가하고 Service에 전달한다.

```java
public ResponseEntity<ApiResponse<EntryResponse>> enter(
    @CookieValue(name = CUSTOMER_TOKEN_COOKIE, required = false) String rawToken,
    @Parameter(
            description = "방문할 매장 코드. 생략하면 기본 매장 MCM-SEOUL을 사용합니다.",
            example = "MCM-PARIS")
        @RequestParam(required = false)
        String storeCode) {
  CustomerVisitService.EntryResult result = service.enter(rawToken, storeCode);
```

필요한 `io.swagger.v3.oas.annotations.Parameter`와 `org.springframework.web.bind.annotation.RequestParam` import만 추가한다.

- [x] **Step 6: 관련 단위·HTTP 테스트 통과 확인**

Run:

```bash
./gradlew test --tests '*CustomerVisitServiceTest' --tests '*CustomerVisitControllerTest' --stacktrace --no-daemon
```

Expected: 기본 서울 선택, 파리·뮌헨 선택, 빈 코드 400, 미등록 코드 404, 기존 쿠키·온보딩 테스트가 모두 PASS.

- [x] **Step 7: API 변경 커밋**

```bash
git add src/main/java/com/lionthanflower/application/customer/CustomerVisitService.java src/main/java/com/lionthanflower/infrastructure/web/customer/CustomerVisitController.java src/test/java/com/lionthanflower/application/customer/CustomerVisitServiceTest.java src/test/java/com/lionthanflower/infrastructure/web/customer/CustomerVisitControllerTest.java
git commit -m "62 feat: 고객 방문 매장 선택 지원"
```

---

### Task 3: 실제 DB·HTTP 매장 연결 통합 검증

**Files:**
- Create: `src/test/java/com/lionthanflower/infrastructure/web/customer/CustomerVisitStoreIntegrationTest.java`
- Modify: `docs/tasks/62/plan.md`

**Interfaces:**
- Consumes: Task 1의 V9 매장 데이터와 Task 2의 `POST /api/customers/visits?storeCode=...`.
- Produces: 실제 HTTP → Service → JPA → PostgreSQL 흐름에서 선택한 Store ID가 Visit에 저장된다는 검증 증거.

- [x] **Step 1: PostgreSQL 통합 테스트 작성**

새 파일은 `PostgreSqlContainerSupport`를 상속하고 `@SpringBootTest`, `@AutoConfigureMockMvc`, `@Transactional`을 사용한다. 첫 줄에는 다음 역할 주석을 둔다.

```java
// 실제 고객 방문 생성 요청이 선택한 매장과 도시 정보에 연결되는지 검증하는 통합 테스트
```

MockMvc 응답의 `data.visitId`를 `ObjectMapper`로 읽고 실제 Repository를 조회한다.

```java
@Test
void 고객_방문은_선택한_뮌헨_매장에_연결된다() throws Exception {
  MvcResult result =
      mockMvc
          .perform(post("/api/customers/visits").param("storeCode", "MCM-MUNICH"))
          .andExpect(status().isCreated())
          .andReturn();

  UUID visitId =
      UUID.fromString(
          objectMapper
              .readTree(result.getResponse().getContentAsString())
              .path("data")
              .path("visitId")
              .asText());
  Visit visit = visitRepository.findById(visitId).orElseThrow();
  Store store = storeRepository.findById(visit.getStoreId()).orElseThrow();

  assertThat(store.getCode()).isEqualTo("MCM-MUNICH");
  assertThat(store.getCityCode()).isEqualTo("MUNICH");
}
```

같은 클래스에서 `GET /api/stores?query=MCM-`가 `MCM-SEOUL`, `MCM-PARIS`, `MCM-MUNICH`를 반환하는지 확인한다.

- [x] **Step 2: 생성 OpenAPI의 매장 선택 계약 검증 작성**

`GET /v3/api-docs` 결과를 `ObjectMapper`로 읽고 `/api/customers/visits` POST 파라미터에서 이름이 `storeCode`인 항목을 찾는다. 다음을 검증한다.

```java
assertThat(storeCodeParameter.path("in").asText()).isEqualTo("query");
assertThat(storeCodeParameter.path("required").asBoolean()).isFalse();
assertThat(storeCodeParameter.path("description").asText()).contains("기본 매장");
assertThat(storeCodeParameter.path("example").asText()).isEqualTo("MCM-PARIS");
```

- [x] **Step 3: 통합 테스트 통과 확인**

Run:

```bash
./gradlew test --tests '*CustomerVisitStoreIntegrationTest' --tests '*CustomerArcCityIntegrationTest' --stacktrace --no-daemon
```

Expected: 선택한 뮌헨 Store ID의 Visit 저장, 세 매장 검색, OpenAPI 계약, 기존 Arc 도시 응답이 모두 PASS.

- [x] **Step 4: 통합 검증 커밋**

```bash
git add src/test/java/com/lionthanflower/infrastructure/web/customer/CustomerVisitStoreIntegrationTest.java docs/tasks/62/plan.md
git commit -m "62 test: 방문 매장 선택 통합 검증 추가"
```

---

### Task 4: 전체 회귀와 변경 검토

**Files:**
- Modify: `docs/tasks/62/plan.md`

**Interfaces:**
- Consumes: Tasks 1~3의 마이그레이션, API, 테스트 커밋.
- Produces: 전체 회귀·포맷·공백 검사 결과와 최종 diff 검토 기록.

- [x] **Step 1: 전체 테스트 실행**

Run:

```bash
./gradlew test --stacktrace --no-daemon
```

Expected: 모든 테스트 PASS, 실패·오류·스킵 0개.

- [x] **Step 2: 포맷과 공백 검사 실행**

Run:

```bash
./gradlew spotlessCheck --no-daemon
git diff --check main...HEAD
```

Expected: 두 검사 모두 종료 코드 0.

- [x] **Step 3: 코드 리뷰 그래프와 diff 검토**

저장소의 `build-graph`를 실행해 코드 리뷰 그래프를 최신화하고 결과를 확인한다. 이후 `git diff --stat main...HEAD`, `git diff main...HEAD`로 이슈 #62 외 변경이 없는지, 공개 API·DDL·오류 계약과 테스트가 일치하는지 검토한다.

- [x] **Step 4: 계획 문서에 실제 검증 결과 기록**

완료된 체크박스를 `[x]`로 바꾸고 `검증 결과` 절에 실제 실행 명령, 테스트 수, 결과, 발견 사항과 남은 위험을 기록한다. 결과를 추정해서 쓰지 않는다.

- [x] **Step 5: 검증 기록 커밋**

```bash
git add docs/tasks/62/plan.md
git commit -m "62 docs: 구현 및 검증 결과 기록"
```

## 검증 결과.

- 마이그레이션 RED. V9를 추가하기 전에 `./gradlew test --tests '*StoreCityMigrationTest' --stacktrace --no-daemon`을 실행하여 PostgreSQL·MySQL의 신규 매장 검증 4건이 실패하는 것을 확인했다.
- 마이그레이션 GREEN. V9 추가 후 같은 명령이 성공했다. 파리·뮌헨의 고정 UUID와 도시 코드, 동일 코드 기존 행의 UUID·값 보존, Flyway 재실행 시 적용 건수 0을 PostgreSQL 17과 MySQL 8.4에서 확인했다.
- API RED. `enter(String, String)` 테스트를 먼저 작성한 뒤 관련 테스트 컴파일에서 기존 시그니처로 인한 오류 11건을 확인했다.
- API GREEN. `./gradlew test --tests '*CustomerVisitServiceTest' --tests '*CustomerVisitControllerTest' --stacktrace --no-daemon`이 성공했다. 기본 서울 매장, 명시적 매장, 빈 코드 400, 미등록 코드 404, 기본 매장 누락 500 계약을 확인했다.
- 통합 검증. `./gradlew test --tests '*CustomerVisitStoreIntegrationTest' --tests '*CustomerArcCityIntegrationTest' --stacktrace --no-daemon`이 성공했다. 실제 HTTP·JPA 흐름의 뮌헨 방문 연결, 세 매장 검색, Springdoc 계약과 Arc 목록·상세의 `SEOUL`, `PARIS`, `MUNICH` 반환을 확인했다.
- 계획에서 단일 `MCM-` 검색으로 세 매장을 한 번에 검증하려 했으나, 공유 테스트 DB의 다른 `MCM-*` 데이터에 영향을 받지 않도록 각 고유 코드로 검색하는 파라미터화 테스트로 변경했다.
- 전체 회귀. `./gradlew test --stacktrace --no-daemon` 결과 287개 테스트가 성공했고 실패·오류·스킵은 모두 0개였다.
- 포맷. `./gradlew spotlessCheck --no-daemon`이 성공했다.
- 공백. `git diff --check origin/main...HEAD`가 종료 코드 0으로 성공했다.
- 리뷰 그래프. 저장소와 실행 경로에서 `build-graph`를 찾지 못해 실행할 수 없었다. 대신 최신 `origin/main`과의 전체 diff, 이슈 #62, 설계 문서와 공개 API·DDL·오류·테스트 계약을 직접 대조했고 범위 이탈이나 미해결 finding은 발견하지 못했다.
- 리뷰 제약. 현재 작업의 위임 요청이 없어 별도 리뷰 에이전트는 사용하지 않고 주 에이전트가 체크리스트 기반 리뷰를 수행했다.
- 남은 운영 조건. 배포 환경에서 Flyway V9가 적용되어야 하며 프론트엔드는 정확한 대문자 코드 `MCM-PARIS` 또는 `MCM-MUNICH`를 전달해야 한다.
