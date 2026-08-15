// Arc 생성 당시 입력과 OpenAI 결과의 전체 스냅샷을 관리하는 엔티티
package com.lionthanflower.domain.arc.entity;

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
    name = "arc_revisions",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_arc_revisions_arc_number",
            columnNames = {"arc_id", "revision_number"}))
public class ArcRevision extends BaseEntity {

  @jakarta.persistence.Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "arc_id", nullable = false, length = 36)
  private UUID arcId;

  @Column(name = "revision_number", nullable = false)
  private int revisionNumber;

  @Column(name = "input_snapshot", nullable = false, columnDefinition = "TEXT")
  private String inputSnapshot;

  @Column(name = "generated_content", columnDefinition = "TEXT")
  private String generatedContent;

  @Column(name = "template_version", nullable = false, length = 100)
  private String templateVersion;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 40)
  private ArcRevisionStatus status;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "created_by_staff_id", nullable = false, length = 36)
  private UUID createdByStaffId;

  @Column(name = "failure_code", length = 100)
  private String failureCode;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "generated_at")
  private Instant generatedAt;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "shared_at")
  private Instant sharedAt;

  protected ArcRevision() {}

  private ArcRevision(
      UUID id,
      UUID arcId,
      int revisionNumber,
      String inputSnapshot,
      String templateVersion,
      UUID createdByStaffId) {
    this.id = id;
    this.arcId = arcId;
    this.revisionNumber = revisionNumber;
    this.inputSnapshot = inputSnapshot;
    this.templateVersion = templateVersion;
    this.createdByStaffId = createdByStaffId;
    this.status = ArcRevisionStatus.GENERATING;
  }

  public static ArcRevision start(
      UUID arcId,
      int revisionNumber,
      String inputSnapshot,
      String templateVersion,
      UUID createdByStaffId) {
    requireUuid(arcId, "Arc ID");
    if (revisionNumber < 1) {
      throw new IllegalArgumentException("리비전 번호는 1 이상이어야 합니다.");
    }
    requireUuid(createdByStaffId, "생성 직원 ID");
    return new ArcRevision(
        UUID.randomUUID(),
        arcId,
        revisionNumber,
        requireText(inputSnapshot, "Arc 입력 스냅샷"),
        requireText(templateVersion, "Arc 프레임 버전"),
        createdByStaffId);
  }

  public void complete(String generatedContent, Instant generatedAt) {
    requireStatus(ArcRevisionStatus.GENERATING, "생성 중인 Arc 리비전만 완료할 수 있습니다.");
    String normalizedGeneratedContent = requireText(generatedContent, "Arc 생성 결과");
    requireInstant(generatedAt, "Arc 생성 완료 시각");
    this.failureCode = null;
    this.generatedContent = normalizedGeneratedContent;
    this.generatedAt = generatedAt;
    this.status = ArcRevisionStatus.READY;
  }

  public void fail(String failureCode) {
    requireStatus(ArcRevisionStatus.GENERATING, "생성 중인 Arc 리비전만 실패 처리할 수 있습니다.");
    this.failureCode = requireText(failureCode, "Arc 생성 실패 코드");
    this.generatedContent = null;
    this.generatedAt = null;
    this.status = ArcRevisionStatus.FAILED;
  }

  void markShared(Instant sharedAt) {
    requireStatus(ArcRevisionStatus.READY, "생성이 완료된 Arc 리비전만 공유할 수 있습니다.");
    requireInstant(sharedAt, "Arc 공유 시각");
    this.sharedAt = sharedAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getArcId() {
    return arcId;
  }

  public int getRevisionNumber() {
    return revisionNumber;
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

  public ArcRevisionStatus getStatus() {
    return status;
  }

  public UUID getCreatedByStaffId() {
    return createdByStaffId;
  }

  public String getFailureCode() {
    return failureCode;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public Instant getSharedAt() {
    return sharedAt;
  }

  private void requireStatus(ArcRevisionStatus expected, String message) {
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
