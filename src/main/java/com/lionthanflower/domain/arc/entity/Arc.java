// 방문별 최종 Arc 이미지와 고객 확정 상태를 관리하는 엔티티
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
  @Column(name = "customer_id", nullable = false, length = 36)
  private UUID customerId;

  @Column(name = "image_object_key", nullable = false, length = 1024)
  private String imageObjectKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 40)
  private ArcStatus status;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "created_by_staff_id", nullable = false, length = 36)
  private UUID createdByStaffId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "last_modified_by_staff_id", nullable = false, length = 36)
  private UUID lastModifiedByStaffId;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "confirmed_at")
  private Instant confirmedAt;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "finalized_at")
  private Instant finalizedAt;

  protected Arc() {}

  private Arc(
      UUID id, UUID visitId, UUID customerId, UUID createdByStaffId, String imageObjectKey) {
    this.id = id;
    this.visitId = visitId;
    this.customerId = customerId;
    this.createdByStaffId = createdByStaffId;
    this.lastModifiedByStaffId = createdByStaffId;
    this.imageObjectKey = imageObjectKey;
    this.status = ArcStatus.DRAFT;
  }

  public static Arc create(
      UUID visitId, UUID customerId, UUID createdByStaffId, String imageObjectKey) {
    requireUuid(visitId, "방문 ID");
    requireUuid(customerId, "고객 ID");
    requireUuid(createdByStaffId, "생성 직원 ID");
    return new Arc(
        UUID.randomUUID(),
        visitId,
        customerId,
        createdByStaffId,
        requireText(imageObjectKey, "Arc 이미지 객체 키"));
  }

  public void replaceImage(String imageObjectKey, UUID staffId) {
    requireStatus(ArcStatus.DRAFT, "수정 중인 Arc만 이미지를 변경할 수 있습니다.");
    requireUuid(staffId, "수정 직원 ID");
    this.imageObjectKey = requireText(imageObjectKey, "Arc 이미지 객체 키");
    this.lastModifiedByStaffId = staffId;
  }

  public void confirm(Instant confirmedAt) {
    requireStatus(ArcStatus.DRAFT, "수정 중인 Arc만 고객이 확정할 수 있습니다.");
    requireInstant(confirmedAt, "Arc 확정 시각");
    this.confirmedAt = confirmedAt;
    this.status = ArcStatus.CONFIRMED;
  }

  public void finalizeArc(Instant finalizedAt) {
    requireStatus(ArcStatus.CONFIRMED, "고객이 확정한 Arc만 최종 저장할 수 있습니다.");
    requireInstant(finalizedAt, "Arc 최종 저장 시각");
    this.finalizedAt = finalizedAt;
    this.status = ArcStatus.FINALIZED;
  }

  public UUID getId() {
    return id;
  }

  public UUID getVisitId() {
    return visitId;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public String getImageObjectKey() {
    return imageObjectKey;
  }

  public ArcStatus getStatus() {
    return status;
  }

  public UUID getCreatedByStaffId() {
    return createdByStaffId;
  }

  public UUID getLastModifiedByStaffId() {
    return lastModifiedByStaffId;
  }

  public Instant getConfirmedAt() {
    return confirmedAt;
  }

  public Instant getFinalizedAt() {
    return finalizedAt;
  }

  private void requireStatus(ArcStatus expected, String message) {
    if (status != expected) {
      throw new IllegalStateException(message);
    }
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

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + "은 비어 있을 수 없습니다.");
    }
    return value.trim();
  }
}
