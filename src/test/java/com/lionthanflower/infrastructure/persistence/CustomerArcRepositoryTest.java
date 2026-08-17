// 고객별 공개 Arc Repository의 실제 JPA 조회 계약을 검증하는 통합 테스트
package com.lionthanflower.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionthanflower.domain.arc.entity.Arc;
import com.lionthanflower.domain.arc.entity.ArcStatus;
import com.lionthanflower.support.PostgreSqlContainerSupport;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class CustomerArcRepositoryTest extends PostgreSqlContainerSupport {

  private static final Timestamp TIMESTAMP = Timestamp.valueOf("2026-08-15 18:00:00");

  @Autowired private ArcRepository arcRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void 고객별_공개_Arc를_번호_내림차순으로_조회하고_소유권과_공개_상태를_제한한다() {
    UUID storeId = UUID.randomUUID();
    UUID staffId = UUID.randomUUID();
    UUID customerId = UUID.randomUUID();
    UUID otherCustomerId = UUID.randomUUID();
    insertStore(storeId);
    insertStaff(staffId, storeId);
    insertCustomer(customerId);
    insertCustomer(otherCustomerId);

    UUID finalizedArcId = insertArc(customerId, staffId, storeId, ArcStatus.FINALIZED, 2);
    UUID sharedArcId = insertArc(customerId, staffId, storeId, ArcStatus.SHARED, 1);
    UUID draftArcId = insertArc(customerId, staffId, storeId, ArcStatus.DRAFT, null);
    UUID otherCustomerArcId = insertArc(otherCustomerId, staffId, storeId, ArcStatus.SHARED, 3);

    List<Arc> result =
        arcRepository.findByCustomerIdAndStatusInOrderByArcNumberDesc(
            customerId, List.of(ArcStatus.SHARED, ArcStatus.FINALIZED));

    assertThat(result).extracting(Arc::getId).containsExactly(finalizedArcId, sharedArcId);
    assertThat(
            arcRepository.findByIdAndCustomerIdAndStatusIn(
                finalizedArcId, customerId, List.of(ArcStatus.SHARED, ArcStatus.FINALIZED)))
        .isPresent()
        .get()
        .extracting(Arc::getCustomerId)
        .isEqualTo(customerId);
    assertThat(
            arcRepository.findByIdAndCustomerIdAndStatusIn(
                otherCustomerArcId, customerId, List.of(ArcStatus.SHARED, ArcStatus.FINALIZED)))
        .isEmpty();
    assertThat(
            arcRepository.findByIdAndCustomerIdAndStatusIn(
                draftArcId, customerId, List.of(ArcStatus.SHARED, ArcStatus.FINALIZED)))
        .isEmpty();
  }

  private UUID insertArc(
      UUID customerId, UUID staffId, UUID storeId, ArcStatus status, Integer arcNumber) {
    UUID visitId = UUID.randomUUID();
    UUID purchaseId = UUID.randomUUID();
    UUID arcId = UUID.randomUUID();
    insertVisit(visitId, customerId, storeId, staffId);
    jdbcTemplate.update(
        "insert into purchases (id, visit_id, created_at, updated_at) values (?, ?, ?, ?)",
        purchaseId,
        visitId,
        TIMESTAMP,
        TIMESTAMP);
    Instant sharedAt = Instant.parse("2026-08-15T12:00:00Z");
    Instant finalizedAt = status == ArcStatus.FINALIZED ? sharedAt.plusSeconds(60) : null;
    jdbcTemplate.update(
        "insert into arcs (id, visit_id, purchase_id, customer_id, created_by_staff_id,"
            + " arc_number, status, shared_at, finalized_at, created_at, updated_at)"
            + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        arcId,
        visitId,
        purchaseId,
        customerId,
        staffId,
        arcNumber,
        status.name(),
        sharedAt,
        finalizedAt,
        TIMESTAMP,
        TIMESTAMP);
    return arcId;
  }

  private void insertStore(UUID storeId) {
    jdbcTemplate.update(
        "insert into stores (id, name, code, country_code, created_at, updated_at)"
            + " values (?, ?, ?, ?, ?, ?)",
        storeId,
        "MCM HAUS",
        "mcm-haus-" + storeId,
        "KR",
        TIMESTAMP,
        TIMESTAMP);
  }

  private void insertStaff(UUID staffId, UUID storeId) {
    jdbcTemplate.update(
        "insert into staff (id, store_id, name, token_hash, created_at, updated_at)"
            + " values (?, ?, ?, ?, ?, ?)",
        staffId,
        storeId,
        "김회윤",
        "staff-token-hash-" + staffId,
        TIMESTAMP,
        TIMESTAMP);
  }

  private void insertCustomer(UUID customerId) {
    jdbcTemplate.update(
        "insert into customers (id, name, token_hash, created_at, updated_at)"
            + " values (?, ?, ?, ?, ?)",
        customerId,
        "고객-" + customerId,
        "customer-token-hash-" + customerId,
        TIMESTAMP,
        TIMESTAMP);
  }

  private void insertVisit(UUID visitId, UUID customerId, UUID storeId, UUID staffId) {
    jdbcTemplate.update(
        "insert into visits (id, customer_id, store_id, staff_id, service_language,"
            + " interaction_style, additional_request, status, purchase_decision,"
            + " purchase_decided_by_staff_id, purchase_decided_at, matched_at, version,"
            + " created_at, updated_at) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        visitId,
        customerId,
        storeId,
        staffId,
        "EN",
        "STAFF_RECOMMENDATION",
        null,
        "ARC_IN_PROGRESS",
        "PURCHASED",
        staffId,
        TIMESTAMP,
        TIMESTAMP,
        0L,
        TIMESTAMP,
        TIMESTAMP);
  }
}
