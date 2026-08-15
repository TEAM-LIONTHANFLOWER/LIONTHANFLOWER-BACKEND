# ADR-002. Aggregate 간 UUID 참조

- 상태. Accepted
- 결정일. 2026-08-15
- 범위. 도메인 경계, 식별자, JPA와 데이터베이스 참조 방식

## 문맥

Store, Staff, Customer, Visit, Product, Purchase, Arc, Visit Memory와 Myself는 서로 다른 생명주기를 가진다. 여러 도메인을 JPA 객체 연관관계로 묶으면 지연 로딩과 영속성 전파가 도메인 규칙과 섞일 수 있다. PostgreSQL과 MySQL에서 같은 migration을 사용하려면 UUID 표현도 공통이어야 한다.

## 결정

- 주요 엔티티의 식별자는 애플리케이션에서 생성한 Java `UUID`를 사용한다.
- 데이터베이스 PK와 외래 키 컬럼은 `CHAR(36)`으로 저장한다.
- JPA UUID 필드는 Hibernate `@JdbcTypeCode(SqlTypes.CHAR)`로 문자형 저장을 명시한다.
- Aggregate 간에는 `@ManyToOne`, `@OneToMany`, `@OneToOne` 같은 객체 연관관계를 만들지 않고 UUID 필드로만 참조한다.
- 참조 무결성은 Flyway migration의 데이터베이스 외래 키로 보장한다.
- 다른 Aggregate의 존재 여부와 같은 매장 소속 여부는 Application Service가 Repository 조회로 검증한다.
- JPA cascade와 DB `ON DELETE CASCADE`는 사용하지 않는다.

## 적용 예

- `Visit`은 `customerId`, `storeId`, `staffId`를 UUID 필드로 가진다.
- `ProductVariant`는 `productId`를, `Purchase`는 `visitId`를, `PurchaseItem`은 `purchaseId`와 `productVariantId`를 가진다.
- `Arc`는 `visitId`, `purchaseId`, `customerId`, `createdByStaffId`, `sharedRevisionId`, `finalRevisionId`를 가진다.
- `ArcRevision`은 `arcId`와 `createdByStaffId`를, `VisitMemory`는 `visitId`, `customerId`, `createdByStaffId`를 가진다.
- `Myself`는 `customerId`, `visitId`를 UUID 필드로 가진다.
- 데이터베이스는 이 식별자 컬럼에 외래 키를 선언하지만 엔티티는 다른 Aggregate 객체를 필드로 보유하지 않는다.

## 대안

### JPA 객체 연관관계 사용

조회 코드는 편해질 수 있지만 객체 그래프와 로딩 전략이 Aggregate 경계를 넘는다. 이번 MVP에서는 명시적인 ID 참조와 서비스 조합을 선택한다.

### 데이터베이스 native UUID 타입 사용

PostgreSQL에서는 효율적일 수 있지만 MySQL과 migration 타입을 별도로 관리해야 한다. 두 데이터베이스에서 동일한 스키마를 유지하기 위해 `CHAR(36)`을 사용한다.

## 결과

### 장점

- Aggregate가 서로의 객체 생명주기와 영속성 상태에 직접 의존하지 않는다.
- API나 배치 작업에서 필요한 참조만 명시적으로 조회할 수 있다.
- PostgreSQL과 MySQL에서 UUID 표현과 Flyway 스키마를 동일하게 유지할 수 있다.
- 예기치 않은 cascade 저장과 삭제를 예방할 수 있다.

### 비용과 제한

- 관련 객체가 필요할 때 Application Service가 여러 Repository를 직접 조합해야 한다.
- 매장 일치와 담당 직원 검증을 서비스에서 빠뜨리지 않도록 테스트해야 한다.
- `CHAR(36)`은 PostgreSQL native UUID보다 저장 공간과 인덱스 효율이 불리할 수 있다.
- 외래 키만으로는 직원·방문·매장의 교차 행 규칙을 완전히 표현할 수 없다.

## 구현 근거

- `domain/*/entity/*.java`
- `global/entity/BaseEntity.java`
- `V2__rebuild_domain_for_ia.sql`
- `InitialDomainPersistenceTest.java`
