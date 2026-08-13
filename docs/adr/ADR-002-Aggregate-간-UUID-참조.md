# ADR-002. Aggregate 간 UUID 참조

- 상태. Accepted
- 결정일. 2026-08-14
- 범위. 도메인 경계, 식별자, JPA와 데이터베이스 참조 방식

## 문맥

현재 도메인은 Store, Staff, StoreDevice, Customer, Visit, Arc, Myself로 나뉜다. Visit와 Arc처럼 여러 도메인이 함께 사용되는 객체를 JPA 객체 연관관계로 연결하면 지연 로딩, 영속성 컨텍스트 전파, 삭제 전파가 도메인 규칙과 섞일 수 있다.

또한 PostgreSQL과 MySQL에서 같은 migration을 사용하기 위해 애플리케이션이 생성한 UUID를 데이터베이스에서 이식 가능한 문자열 타입으로 저장해야 한다.

## 결정

- 모든 주요 엔티티의 식별자는 애플리케이션에서 생성한 Java `UUID`를 사용한다.
- 데이터베이스 PK와 외래 키 컬럼은 `CHAR(36)`으로 저장한다.
- JPA UUID 필드는 Hibernate `@JdbcTypeCode(SqlTypes.CHAR)`로 문자형 저장을 명시한다.
- 애그리게이트 간에는 `@ManyToOne`, `@OneToMany`, `@OneToOne` 같은 JPA 객체 연관관계를 만들지 않고 UUID 필드로만 참조한다.
- 참조 무결성은 Flyway migration의 데이터베이스 외래 키로 보장한다.
- 다른 애그리게이트의 존재 여부, 활성 상태, 같은 매장 소속 여부는 Application Service가 Repository 조회를 통해 검증한다.
- 애그리게이트 삭제 시 JPA cascade나 DB `ON DELETE CASCADE`를 사용하지 않는다.

## 적용 예

- `Visit`은 `customerId`, `storeId`, `staffId`, `storeDeviceId`를 UUID 필드로 가진다.
- `Arc`는 `visitId`, `customerId`, `createdByStaffId`, `lastModifiedByStaffId`를 UUID 필드로 가진다.
- `Myself`는 `customerId`, `visitId`를 UUID 필드로 가진다.
- 데이터베이스는 이 식별자 컬럼에 외래 키를 선언하지만 엔티티는 다른 애그리게이트 객체를 필드로 보유하지 않는다.

## 대안

### JPA 객체 연관관계 사용

조회 코드는 편해질 수 있지만, 애그리게이트 경계를 넘어 객체 그래프가 확장되고 로딩 전략과 트랜잭션 범위가 도메인 모델에 영향을 준다. 이번 MVP에서는 명시적인 ID 참조와 서비스 조합을 선택한다.

### 데이터베이스 native UUID 타입 사용

PostgreSQL에서는 효율적이지만 MySQL과 migration 타입을 별도로 관리해야 한다. 두 데이터베이스에서 같은 스키마를 유지하기 위해 `CHAR(36)`을 사용한다.

## 결과

### 장점

- 애그리게이트가 서로의 객체 생명주기와 영속성 상태에 직접 의존하지 않는다.
- API나 배치 작업에서 필요한 참조만 명시적으로 조회할 수 있다.
- PostgreSQL과 MySQL에서 동일한 UUID 표현과 Flyway 스키마를 사용할 수 있다.
- JPA의 예기치 않은 cascade 저장이나 삭제를 예방할 수 있다.

### 비용과 제한

- 관련 객체가 필요할 때 Application Service가 여러 Repository를 직접 조합해야 한다.
- 매장 일치와 활성 직원 검증처럼 여러 애그리게이트에 걸친 규칙을 서비스에서 빠뜨리지 않도록 테스트해야 한다.
- `CHAR(36)`은 PostgreSQL native UUID보다 저장 공간과 인덱스 효율이 불리할 수 있다.
- 외래 키만으로는 직원·단말·방문이 같은 매장에 속한다는 교차 행 규칙을 완전히 표현할 수 없다.

## 구현 근거

- `domain/*/entity/*.java`
- `global/entity/BaseEntity.java`
- `V1__create_initial_domain_tables.sql`
- `InitialDomainPersistenceTest.java`

