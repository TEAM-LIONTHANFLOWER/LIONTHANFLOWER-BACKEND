// 구매 방문의 Arc 공유와 고객 최종 저장 상태를 관리하는 엔티티
package com.lionthanflower.domain.arc.entity;

import com.lionthanflower.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "arcs",
    uniqueConstraints = @UniqueConstraint(name = "uk_arcs_visit_id", columnNames = "visit_id"),
    indexes = @Index(name = "idx_arcs_customer_created_at", columnList = "customer_id, created_at"))
public class Arc extends BaseEntity {

  @jakarta.persistence.Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "visit_id", nullable = false, unique = true, length = 36)
  private UUID visitId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "purchase_id", nullable = false, unique = true, length = 36)
  private UUID purchaseId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "customer_id", nullable = false, length = 36)
  private UUID customerId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "created_by_staff_id", nullable = false, length = 36)
  private UUID createdByStaffId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "shared_revision_id", length = 36)
  private UUID sharedRevisionId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "final_revision_id", length = 36)
  private UUID finalRevisionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 40)
  private ArcStatus status;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "shared_at")
  private Instant sharedAt;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "finalized_at")
  private Instant finalizedAt;

  protected Arc() {}

  private Arc(UUID id, UUID visitId, UUID purchaseId, UUID customerId, UUID createdByStaffId) {
    this.id = id;
    this.visitId = visitId;
    this.purchaseId = purchaseId;
    this.customerId = customerId;
    this.createdByStaffId = createdByStaffId;
    this.status = ArcStatus.DRAFT;
  }

  public static Arc create(UUID visitId, UUID purchaseId, UUID customerId, UUID createdByStaffId) {
    requireUuid(visitId, "방문 ID");
    requireUuid(purchaseId, "구매 ID");
    requireUuid(customerId, "고객 ID");
    requireUuid(createdByStaffId, "생성 직원 ID");
    return new Arc(UUID.randomUUID(), visitId, purchaseId, customerId, createdByStaffId);
  }

  public void share(ArcRevision revision, Instant sharedAt) {
    if (status == ArcStatus.FINALIZED) {
      throw new IllegalStateException("최종 저장된 Arc는 다시 공유할 수 없습니다.");
    }
    if (revision == null || !id.equals(revision.getArcId())) {
      throw new IllegalArgumentException("같은 Arc의 리비전만 공유할 수 있습니다.");
    }
    if (revision.getStatus() != ArcRevisionStatus.READY) {
      throw new IllegalStateException("생성이 완료된 Arc 리비전만 공유할 수 있습니다.");
    }
    revision.markShared(sharedAt);
    this.sharedRevisionId = revision.getId();
    this.sharedAt = sharedAt;
    this.status = ArcStatus.SHARED;
  }

  public void finalizeSharedRevision(Instant finalizedAt) {
    if (status != ArcStatus.SHARED || sharedRevisionId == null) {
      throw new IllegalStateException("고객에게 공유된 Arc만 최종 저장할 수 있습니다.");
    }
    requireInstant(finalizedAt, "Arc 최종 저장 시각");
    this.finalRevisionId = sharedRevisionId;
    this.finalizedAt = finalizedAt;
    this.status = ArcStatus.FINALIZED;
  }

  public UUID getId() {
    return id;
  }

  public UUID getVisitId() {
    return visitId;
  }

  public UUID getPurchaseId() {
    return purchaseId;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public UUID getCreatedByStaffId() {
    return createdByStaffId;
  }

  public UUID getSharedRevisionId() {
    return sharedRevisionId;
  }

  public UUID getFinalRevisionId() {
    return finalRevisionId;
  }

  public ArcStatus getStatus() {
    return status;
  }

  public Instant getSharedAt() {
    return sharedAt;
  }

  public Instant getFinalizedAt() {
    return finalizedAt;
  }

  private static void requireUuid(UUID value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(fieldName + "는 null일 수 없습니다.");
    }
  }

  private static void requireInstant(Instant value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다.");
    }
  }
}
