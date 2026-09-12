# 이슈 #57. Arc 구매 매장 도시 정보 추가.

## 목적과 구현 기준.

- 이슈. https://github.com/TEAM-LIONTHANFLOWER/LIONTHANFLOWER-BACKEND/issues/57
- 기준 브랜치. `develop`의 `30f54dc`에서 생성한 `feat/57`.
- 구매 매장은 기존 고객 Arc 조회와 동일하게 `Arc.visitId → Visit.storeId → Store`로 결정한다. 직원 입력의 `purchaseStore` 문자열은 도시 조회 기준으로 사용하지 않는다.
- Store에 nullable `cityCode`를 추가한다. 공백 제거와 `Locale.ROOT` 대문자 정규화를 적용하고, 100자 이하 문자열을 사용한다. 별도 국제 표준이나 도시 목록이 없으므로 추가 형식 제한은 두지 않는다. 예시는 `SEOUL`, `PARIS`, `NEW_YORK`이며 국제 표준 코드를 뜻하지 않는다.
- 고객 Arc 목록과 상세 응답에 `cityCode`를 추가한다. 도시 미등록은 명시적인 JSON `null`로 반환해 기본 편지지를 선택할 수 있게 한다.
- 알려진 도시와 실제 준비된 디자인의 범위는 별개다. 미지원 도시 및 null의 기본 디자인 처리는 프론트엔드에서 담당한다.
- V8에서 `stores.city_code VARCHAR(100) NULL`을 추가하고, `code = 'MCM-SEOUL' AND country_code = 'KR' AND city_code IS NULL`인 매장만 `SEOUL`로 보완한다. 다른 매장의 도시는 추측하지 않는다.
- 기존 3인자 `Store.create`는 도시 미등록을 의미하며 유지한다. 도시를 지정하는 4인자 생성 경로를 추가한다.
- 목록·상세는 현재 매장 정보를 반환하는 기존 방식에 맞춘다. 과거 도시 스냅샷, 도시 카탈로그, 매장 관리 API는 추가하지 않는다.
- 도시 필드와 기준은 작업 요청 및 기존 대화의 제안에 따른 구현 가정이다. 별도 프론트엔드 합의가 전달되면 반영한다.

## 작업과 검증.

- [x] V7까지 적용된 독립 PostgreSQL·MySQL 테스트 DB에 기존 매장 데이터를 넣고, V8 적용으로 한 번만 마이그레이션되며 서울만 보완되는 회귀 테스트를 먼저 실행한다.
- [x] `Store.java`와 `V8__add_store_city_code.sql`을 구현한다. 도시 정규화, null·blank와 최대 길이 검증을 `StoreDomainTest`에서 확인한다.
- [x] `CustomerArcQueryService`와 `CustomerArcController`의 목록·상세 DTO 및 매핑에 `cityCode`를 추가하고 Springdoc에 기준·null 동작을 설명한다.
- [x] 기존 Arc 단위·HTTP 테스트를 갱신하고 서울·파리·도시 미등록 조회를 검증한다.
- [x] 실제 PostgreSQL, JPA, 서비스, 컨트롤러, 쿠키 인증을 통과하는 MockMvc 통합 테스트로 서로 다른 도시의 목록·상세 및 기존 도시 없는 Arc 조회를 검증한다. 생성된 OpenAPI의 cityCode 노출도 확인한다.
- [x] `./gradlew test --stacktrace --no-daemon`, `./gradlew spotlessCheck --no-daemon`, `git diff --check`를 실행한다.
- [x] 코드 리뷰 그래프를 갱신하고 독립 Sol 리뷰어가 변경 및 HIGH 검증 증거를 검토한다. 차단 지적이 없을 때 작업 단위를 커밋한다.

## 검증 결과와 발견 사항.

Sol 설계 검토에서 기존 Visit→Store 조회 재사용과 nullable V8 보완을 확인했다. 도시의 표준 형식이 미정이므로 정규식 제한은 생략한다. 새 매장도 도시 미확정을 허용하며 3인자 생성 호환 경로를 유지한다. 마이그레이션 변경은 저장소 규칙에 따라 HIGH 검증을 유지한다.

- RED 확인. V8이 없는 코드에서 PostgreSQL·MySQL 업그레이드 테스트 2개가 실패했고, 도시 필드가 없는 기존 Store/서비스/HTTP 응답에서 회귀 테스트 5개가 실패했다.
- 관련 테스트. `./gradlew test --tests '*StoreDomainTest' --tests '*StoreCityMigrationTest' --tests '*CustomerArcQueryServiceTest' --tests '*CustomerArcControllerTest' --tests '*CustomerArcCityIntegrationTest' --stacktrace --no-daemon` — 33개 성공.
- 최종 전체 테스트. `./gradlew test --stacktrace --no-daemon` — 272개 성공, 실패·오류·스킵 0개.
- 포맷/공백 검사. `./gradlew spotlessCheck --no-daemon`, `git diff --check` — 성공.
- 실제 조회 검증. PostgreSQL에 서로 다른 매장과 SHARED/FINALIZED Arc를 JPA로 저장한 뒤 영속성 컨텍스트를 비우고, 실제 고객 쿠키로 MockMvc 목록·상세를 조회했다. `SEOUL`, `PARIS`, JSON `null`과 기존 편지 본문·제품 응답을 확인했다. 스냅샷의 구매 매장 문자열은 다르게 설정해 Visit의 매장 정보를 사용하는지도 확인했다.
- API 문서 검증. `/v3/api-docs`의 목록·상세 `cityCode`가 `string | null`, 예시 `SEOUL`, 최대 길이 100으로 생성됨을 확인했다.
- 마이그레이션 검증. 독립 PostgreSQL 17과 MySQL 8.4에서 V7→V8 적용 1회, 재실행 0회, 확인된 서울 매장의 도시 보완과 같은 이름의 다른 매장의 null·기존 데이터 보존을 확인했다.
- 코드 리뷰 그래프. 첫 전체 갱신에서는 미추적 새 테스트 2개가 제외된 것을 독립 리뷰가 발견했다. 파일을 스테이징한 뒤 다시 전체 갱신했고, 새 테스트의 노드 3개·6개와 Java 파일 167개, 파싱 오류 0건, `feat/57`의 HEAD 일치를 확인했다.
- 독립 Sol 리뷰. 기능·마이그레이션·API의 차단 결함은 없었고 HIGH 검증이 충분함을 확인했다. 그래프에서 새 테스트가 누락됐다는 지적은 스테이징 후 재갱신으로 해결했으며, 리뷰어가 실제 DB의 노드와 파일 수를 재확인해 해소 판정했다.
