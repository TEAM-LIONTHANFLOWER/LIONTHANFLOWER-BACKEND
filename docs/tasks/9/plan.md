# CodeRabbit Review 반영 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** PR #10에서 확인된 도메인 상태 원자성, 입력 검증, 데이터베이스 무결성 및 테스트 신뢰성 문제를 수정한다.

**Architecture:** 엔티티의 상태 변경은 모든 인자 검증 후 한 번에 수행한다. Arc와 Visit Memory 입력은 임의 JSON 문자열 대신 검증된 불변 입력 객체를 받아 직렬화하며, Arc의 현재 리비전 소속 관계는 도메인과 복합 외래 키 양쪽에서 보장한다.

**Tech Stack:** Java 21, Spring Boot 4.1, JPA, Jackson, Flyway, PostgreSQL, MySQL 8.4, JUnit 5, AssertJ.

## 전역 제약

- IA와 이슈 #9를 코드보다 우선한다.
- PostgreSQL과 MySQL에서 동일한 migration을 사용한다.
- V2는 병합 전이며 폐기 가능한 초기 개발 데이터만 재구성한다.
- 새 소스 파일 첫 줄에는 역할을 설명하는 한국어 주석을 둔다.
- 각 변경은 실패 테스트를 먼저 확인하고 최소 구현으로 통과시킨다.

---

### Task 1: 상태 변경과 기본 입력 경계 보강

**Files:**
- Modify: `src/test/java/com/lionthanflower/domain/arc/entity/ArcTest.java`
- Modify: `src/test/java/com/lionthanflower/domain/visitmemory/entity/VisitMemoryTest.java`
- Modify: `src/test/java/com/lionthanflower/domain/store/entity/StoreDomainTest.java`
- Modify: `src/test/java/com/lionthanflower/domain/visit/entity/VisitTest.java`
- Modify: `src/test/java/com/lionthanflower/domain/purchase/entity/PurchaseDomainTest.java`
- Modify: `src/main/java/com/lionthanflower/domain/arc/entity/ArcRevision.java`
- Modify: `src/main/java/com/lionthanflower/domain/visitmemory/entity/VisitMemory.java`
- Modify: `src/main/java/com/lionthanflower/domain/store/entity/Store.java`

**Interfaces:**
- `ArcRevision.complete(String, Instant)`와 `VisitMemory.complete(String, Instant)`는 검증 실패 시 객체를 변경하지 않는다.
- `Store.create(String, String, String)`은 ISO 3166-1 alpha-2 코드만 허용한다.

- [x] 원자성, ISO 코드, 상태 전이 및 null 입력 경계 테스트를 추가한다.
- [x] 관련 도메인 테스트를 실행해 예상 실패를 확인한다.
- [x] 모든 값 검증 후 필드를 대입하도록 메서드를 수정한다.
- [x] 관련 도메인 테스트 통과를 확인한다.
- [x] `9 fix: 도메인 입력 검증과 상태 변경 원자성 보강`으로 커밋한다.

### Task 2: 타입이 보장된 Arc와 Visit Memory 입력 스냅샷

**Files:**
- Create: `src/main/java/com/lionthanflower/domain/common/entity/SnapshotJsonSerializer.java`
- Create: `src/main/java/com/lionthanflower/domain/arc/entity/ArcInputSnapshot.java`
- Create: `src/main/java/com/lionthanflower/domain/visitmemory/entity/VisitMemoryInputSnapshot.java`
- Modify: `src/main/java/com/lionthanflower/domain/arc/entity/ArcRevision.java`
- Modify: `src/main/java/com/lionthanflower/domain/visitmemory/entity/VisitMemory.java`
- Modify: `src/test/java/com/lionthanflower/domain/arc/entity/ArcTest.java`
- Modify: `src/test/java/com/lionthanflower/domain/visitmemory/entity/VisitMemoryTest.java`

**Interfaces:**
- `ArcRevision.start(UUID, int, ArcInputSnapshot, String, UUID)`는 구매 Variant ID와 확정 enum으로 구성된 입력을 저장한다.
- `VisitMemory.create(UUID, UUID, UUID, VisitMemoryInputSnapshot, String)`는 제품별 행동, 관심 포인트, 미구매 사유와 메모를 저장한다.
- 기타 입력은 최대 100자, 직원 관찰과 다음 방문 메모는 최대 200자로 제한한다.

- [x] 유효 스냅샷 직렬화와 잘못된 길이 입력 테스트를 추가한다.
- [x] 관련 테스트를 실행해 컴파일 또는 검증 실패를 확인한다.
- [x] 불변 입력 record와 JSON 직렬화를 구현한다.
- [x] 엔티티 생성 API를 타입 입력으로 변경한다.
- [x] 관련 테스트 통과를 확인한다.
- [x] `9 feat: Arc와 Visit Memory 입력 스냅샷 검증`으로 커밋한다.

### Task 3: Arc 리비전 관계 무결성과 persistence 테스트 보정

**Files:**
- Modify: `src/main/resources/db/migration/V2__rebuild_domain_for_ia.sql`
- Modify: `src/main/java/com/lionthanflower/domain/arc/entity/ArcRevision.java`
- Modify: `src/test/java/com/lionthanflower/infrastructure/persistence/InitialDomainPersistenceTest.java`

**Interfaces:**
- `arc_revisions(id, arc_id)`를 복합 참조 대상으로 선언한다.
- `arcs(shared_revision_id, id)`와 `arcs(final_revision_id, id)`가 동일 Arc 소속 리비전만 참조한다.

- [x] 중복 삽입 시 새 PK를 사용하고 다른 Arc의 리비전 참조를 거부하는 persistence 테스트를 추가한다.
- [x] migration 테스트를 실행해 예상 실패를 확인한다.
- [x] PostgreSQL과 MySQL 공통 복합 유일 제약 및 외래 키를 추가한다.
- [x] PostgreSQL과 MySQL migration 테스트 통과를 확인한다.
- [x] `9 fix: Arc 리비전 데이터베이스 무결성 보강`으로 커밋한다.

### Task 4: 파괴적 V2의 적용 범위 문서화와 전체 검증

**Files:**
- Create: `docs/tasks/9/design.md`

- [x] V1 데이터를 의미 있게 자동 변환할 수 없는 이유와 V2의 폐기 가능한 환경 제한을 기록한다.
- [x] `./gradlew test --stacktrace --no-daemon`을 실행한다.
- [x] `./gradlew spotlessCheck build --stacktrace --no-daemon`을 실행한다.
- [x] `git diff --check`와 작업 범위를 확인한다.
- [x] `9 docs: IA 스키마 재구성 적용 범위 명시`로 커밋한다.
- [ ] 브랜치를 푸시하고 PR CI 결과를 확인한다.
