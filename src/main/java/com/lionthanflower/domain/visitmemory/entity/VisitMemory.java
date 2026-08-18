// 미구매 방문의 직원 입력과 OpenAI 결과 스냅샷을 관리하는 엔티티
package com.lionthanflower.domain.visitmemory.entity;

import com.lionthanflower.domain.common.entity.SnapshotJsonSerializer;
import com.lionthanflower.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "visit_memories",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_visit_memories_visit_id", columnNames = "visit_id"))
public class VisitMemory extends BaseEntity {

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

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "created_by_staff_id", nullable = false, length = 36)
  private UUID createdByStaffId;

  @Column(name = "input_snapshot", nullable = false, columnDefinition = "TEXT")
  private String inputSnapshot;

  @Column(name = "generated_content", columnDefinition = "TEXT")
  private String generatedContent;

  @Column(name = "template_version", nullable = false, length = 100)
  private String templateVersion;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 40)
  private VisitMemoryStatus status;

  @Column(name = "failure_code", length = 100)
  private String failureCode;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "generated_at")
  private Instant generatedAt;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "finalized_at")
  private Instant finalizedAt;

  protected VisitMemory() {}

  private VisitMemory(
      UUID id,
      UUID visitId,
      UUID customerId,
      UUID createdByStaffId,
      String inputSnapshot,
      String templateVersion) {
    this.id = id;
    this.visitId = visitId;
    this.customerId = customerId;
    this.createdByStaffId = createdByStaffId;
    this.inputSnapshot = inputSnapshot;
    this.templateVersion = templateVersion;
    this.status = VisitMemoryStatus.DRAFT;
  }

  public static VisitMemory create(
      UUID visitId,
      UUID customerId,
      UUID createdByStaffId,
      VisitMemoryInputSnapshot inputSnapshot,
      String templateVersion) {
    requireUuid(visitId, "방문 ID");
    requireUuid(customerId, "고객 ID");
    requireUuid(createdByStaffId, "생성 직원 ID");
    return new VisitMemory(
        UUID.randomUUID(),
        visitId,
        customerId,
        createdByStaffId,
        SnapshotJsonSerializer.serialize(inputSnapshot),
        requireText(templateVersion, "Visit Memory 프레임 버전"));
  }

  public void startGeneration() {
    if (status != VisitMemoryStatus.DRAFT
        && status != VisitMemoryStatus.READY
        && status != VisitMemoryStatus.FAILED) {
      throw new IllegalStateException("초안, 생성 완료 또는 생성 실패 상태에서만 다시 생성할 수 있습니다.");
    }
    this.generatedContent = null;
    this.generatedAt = null;
    this.finalizedAt = null;
    this.failureCode = null;
    this.status = VisitMemoryStatus.GENERATING;
  }

  public void completeGeneration(String generatedContent, Instant generatedAt) {
    requireGenerating();
    String normalizedGeneratedContent = requireText(generatedContent, "Visit Memory 생성 결과");
    requireInstant(generatedAt, "Visit Memory 생성 완료 시각");
    this.generatedContent = normalizedGeneratedContent;
    this.generatedAt = generatedAt;
    this.finalizedAt = null;
    this.failureCode = null;
    this.status = VisitMemoryStatus.READY;
  }

  public void finalizeMemory(Instant finalizedAt) {
    if (status != VisitMemoryStatus.READY) {
      throw new IllegalStateException("생성이 완료된 Visit Memory만 최종 저장할 수 있습니다.");
    }
    requireInstant(finalizedAt, "Visit Memory 최종 저장 시각");
    this.finalizedAt = finalizedAt;
    this.status = VisitMemoryStatus.FINALIZED;
  }

  public void replaceInput(VisitMemoryInputSnapshot inputSnapshot) {
    if (status != VisitMemoryStatus.READY && status != VisitMemoryStatus.FAILED) {
      throw new IllegalStateException("생성 완료 또는 생성 실패 상태에서만 입력을 수정할 수 있습니다.");
    }
    this.inputSnapshot = SnapshotJsonSerializer.serialize(inputSnapshot);
  }

  public void fail(String failureCode) {
    requireGenerating();
    this.failureCode = requireText(failureCode, "Visit Memory 실패 코드");
    this.generatedContent = null;
    this.generatedAt = null;
    this.finalizedAt = null;
    this.status = VisitMemoryStatus.FAILED;
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

  public UUID getCreatedByStaffId() {
    return createdByStaffId;
  }

  public String getInputSnapshot() {
    return inputSnapshot;
  }

  public String getGeneratedContent() {
    return generatedContent;
  }

  public String getTemplateVersion() {
    return templateVersion;
  }

  public VisitMemoryStatus getStatus() {
    return status;
  }

  public String getFailureCode() {
    return failureCode;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public Instant getFinalizedAt() {
    return finalizedAt;
  }

  private void requireGenerating() {
    if (status != VisitMemoryStatus.GENERATING) {
      throw new IllegalStateException("생성 중인 Visit Memory만 완료하거나 실패 처리할 수 있습니다.");
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
