# 이슈 #62. 파리·뮌헨 매장 및 방문 매장 선택 지원.

## 목적.

- 이슈. https://github.com/TEAM-LIONTHANFLOWER/LIONTHANFLOWER-BACKEND/issues/62
- 고객이 서비스에 진입할 때 매장을 선택하고, 이후 Arc 목록과 상세가 방문에 연결된 매장의 `cityCode`를 반환하게 한다.
- 기존 `POST /api/customers/visits` 호출은 변경 없이 서울 매장을 사용하도록 하위 호환성을 유지한다.

## 검토한 방식.

1. 기존 엔드포인트에 선택적 `storeCode` 쿼리 파라미터를 추가한다.
   - 기존 호출을 깨지 않고 QR 링크에 매장 코드를 포함하기 쉽다.
   - 이번 요구에 필요한 변경만 포함하므로 이 방식을 채택한다.
2. `POST /api/customers/visits`에 요청 본문을 추가한다.
   - 확장성은 있지만 현재 본문이 없는 API의 호출 계약을 크게 바꾸고 기존 클라이언트 처리가 복잡해진다.
3. `/api/stores/{storeCode}/visits`처럼 매장별 새 경로를 추가한다.
   - 리소스 관계는 명확하지만 기존 엔드포인트와 중복되고 프론트엔드 전환 범위가 커진다.

## API 계약.

- `POST /api/customers/visits?storeCode=MCM-PARIS` 형식으로 매장을 선택한다.
- `storeCode`가 없으면 `app.onboarding.store-code` 설정값을 사용한다. 기본값은 기존과 같은 `MCM-SEOUL`이다.
- 명시한 `storeCode`가 존재하면 새 Visit의 `storeId`에 해당 매장 ID를 저장한다.
- 명시한 `storeCode`가 비어 있으면 `COMMON-400`, 존재하지 않으면 `COMMON-404`를 반환한다.
- 기본 설정 코드에 해당하는 매장이 없으면 기존과 같이 서버 설정 오류인 `COMMON-500`을 반환한다.
- 매장 코드는 DB의 고유 `stores.code`와 정확히 일치하는 대문자 계약으로 사용한다. 별도 별칭이나 도시명 기반 검색은 추가하지 않는다.
- 응답 구조와 고객 쿠키 발급·재사용 동작은 변경하지 않는다.
- Springdoc에 선택적 `storeCode`, 기본 동작, 예시를 문서화한다.

## 데이터 변경.

- 새 Flyway `V9` 마이그레이션으로 다음 매장을 추가한다.
  - `MCM Paris`, `MCM-PARIS`, `FR`, `PARIS`.
  - `MCM Munich`, `MCM-MUNICH`, `DE`, `MUNICH`.
- 기존 서울 매장의 고정 UUID 다음 값을 사용해 환경마다 동일한 식별자를 유지한다.
  - 파리. `00000000-0000-0000-0000-000000000002`.
  - 뮌헨. `00000000-0000-0000-0000-000000000003`.
- 동일한 매장 코드가 이미 있으면 기존 행을 수정하거나 덮어쓰지 않고 삽입을 생략한다.
- 적용된 V8은 수정하지 않는다.

## 데이터 흐름.

1. 고객이 선택적 `storeCode`와 함께 서비스 진입 API를 호출한다.
2. Controller가 고객 쿠키와 `storeCode`를 Application Service에 전달한다.
3. Service가 명시된 코드 또는 기본 코드를 사용해 Store를 조회한다.
4. 기존 고객 재사용 또는 신규 고객 생성을 수행한다.
5. 조회한 Store ID로 ONBOARDING Visit을 생성한다.
6. 이후 Arc 조회는 기존 `Arc → Visit → Store` 흐름을 그대로 사용해 `cityCode`를 반환한다.

## 오류와 호환성.

- 비어 있는 매장 코드는 방문을 만들지 않고 `COMMON-400`, 존재하지 않는 매장 코드는 `COMMON-404`로 처리한다.
- 생략 시 사용하는 기본 매장이 존재하지 않으면 운영 설정 문제이므로 기존과 같이 `COMMON-500`으로 처리한다.
- `storeCode`를 생략한 기존 프론트엔드는 계속 서울 방문을 생성한다.
- 기존 Visit과 Arc는 저장된 `storeId`를 그대로 사용하므로 마이그레이션의 영향을 받지 않는다.
- 매장 선택은 공개 진입 API의 기능이며 별도 직원 권한이나 관리 API는 추가하지 않는다.

## 검증.

- Controller 테스트에서 파리·뮌헨 코드 전달, 파라미터 생략, Springdoc 계약을 확인한다.
- Service 테스트에서 명시적 매장 선택, 기본 서울 매장, 존재하지 않는 코드의 `COMMON-404`, Visit의 Store ID를 확인한다.
- PostgreSQL 17과 MySQL 8.4에서 V8→V9 적용, 세 매장 값, 동일 코드 보존, 재실행 시 추가 실행 없음을 확인한다.
- 실제 JPA와 HTTP 흐름을 통과하는 통합 테스트에서 선택한 매장의 Visit과 Arc `cityCode` 연결을 확인한다.
- `./gradlew test --stacktrace --no-daemon`, `./gradlew spotlessCheck --no-daemon`, `git diff --check`를 실행한다.
