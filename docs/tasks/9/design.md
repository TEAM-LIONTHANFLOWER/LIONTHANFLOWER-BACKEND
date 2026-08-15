# 이슈 9 도메인 구조 개편 설계 결정

## 기준

- 고객·직원 IA와 이슈 #9의 확정 사항을 기존 코드보다 우선한다.
- 이번 범위는 해커톤 MVP의 엔티티와 초기 스키마를 확정하는 작업이다.
- PostgreSQL과 MySQL 8.4에서 같은 Flyway DDL과 JPA 매핑을 검증한다.

## 입력 스냅샷

Arc와 Visit Memory 엔티티는 외부에서 조립한 임의 JSON 문자열을 받지 않는다. 각각 `ArcInputSnapshot`, `VisitMemoryInputSnapshot` 불변 입력을 받아 enum, UUID, 컬렉션과 메모 길이를 생성 경계에서 검증한 뒤 JSON으로 직렬화한다.

- Arc는 구매 ProductVariant, 선호 제품군·컬러·스타일, 관심 제품, 구매 기준, 응대·설명·결정 선호와 직원 관찰 메모를 보존한다.
- Visit Memory는 ProductVariant별 고객 행동, 관심 포인트, 미구매 사유와 다음 방문 메모를 보존한다.
- 각 기타 입력은 최대 100자, 직원 관찰과 다음 방문 메모는 최대 200자로 제한한다.
- 재시도는 최초 검증 후 저장된 동일 스냅샷을 사용한다.

## Arc 리비전 무결성

`Arc.share`는 같은 Arc의 READY 리비전만 허용한다. 데이터베이스에서도 `arc_revisions(id, arc_id)`를 복합 참조 대상으로 만들고, Arc의 공유·최종 리비전 외래 키가 `(revision_id, arc_id)` 조합을 참조하도록 제한한다.

## V2 migration 적용 범위

V1의 기존 데이터는 새 IA 구조로 무손실 자동 변환할 수 없다.

- 기존 Arc에는 새 구조에서 필수인 Purchase와 구매 ProductVariant 정보가 없다.
- 기존 Staff에는 개인 기기 인증용 직원별 tokenHash가 없다.
- StoreDevice 토큰을 Staff 토큰으로 옮길 때 직원과 단말의 영구적인 일대일 관계를 보장할 근거가 없다.
- 임의 Purchase, ProductVariant 또는 인증 토큰을 만들어 백필하면 실제 고객 행동과 인증 정보를 왜곡한다.

따라서 V2는 운영 데이터의 순방향 이관 migration이 아니라 폐기 가능한 초기 개발 데이터베이스의 IA 기준 스키마 재구성으로 제한한다. 운영 또는 보존 대상 데이터베이스에는 V2를 적용하지 않는다. 보존 대상 데이터가 생긴 뒤에는 원본 데이터의 의미와 매핑 규칙을 먼저 확정하고 별도의 순방향 migration을 작성해야 한다.

이 제한이 배포 환경에서 보장되지 않는다면 V2의 `DROP TABLE` 사용은 허용할 수 없으며 배포를 중단해야 한다.

## CodeRabbit 리뷰 처리

- MySQL 예약어 `option`은 `size_option`으로 변경했다.
- 상태 완료 메서드는 모든 입력 검증 후 필드를 변경한다.
- 매장 국가는 ISO 3166-1 alpha-2 목록으로 검증한다.
- 입력 스냅샷은 타입 객체로 검증한다.
- Arc 리비전 소속은 도메인과 데이터베이스에서 함께 검증한다.
- persistence 유일 제약 테스트는 중복 삽입에 새로운 기본 키를 사용한다.
- 파괴적 migration 지적은 수용하되, 데이터 이관 규칙 없이 임의 백필하지 않고 적용 환경을 제한한다.
