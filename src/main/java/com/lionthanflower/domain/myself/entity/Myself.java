// 고객 이미지 기반 Myself 생성 작업과 객체 키를 관리하는 엔티티
package com.lionthanflower.domain.myself.entity;

import com.lionthanflower.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "myself_images")
public class Myself extends BaseEntity {

  @jakarta.persistence.Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "customer_id", nullable = false, length = 36)
  private UUID customerId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "visit_id", nullable = false, length = 36)
  private UUID visitId;

  @Enumerated(EnumType.STRING)
  @Column(name = "frame_type", nullable = false, length = 40)
  private FrameType frameType;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 40)
  private MyselfStatus status;

  @Column(name = "source_image_object_key", length = 1024)
  private String sourceImageObjectKey;

  @Column(name = "result_image_object_key", length = 1024)
  private String resultImageObjectKey;

  @Column(name = "failure_code", length = 100)
  private String failureCode;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "completed_at")
  private Instant completedAt;

  protected Myself() {}

  private Myself(
      UUID id, UUID customerId, UUID visitId, FrameType frameType, String sourceImageObjectKey) {
    this.id = id;
    this.customerId = customerId;
    this.visitId = visitId;
    this.frameType = frameType;
    this.sourceImageObjectKey = sourceImageObjectKey;
    this.status = MyselfStatus.PROCESSING;
  }

  public static Myself create(
      UUID customerId, UUID visitId, FrameType frameType, String sourceImageObjectKey) {
    requireUuid(customerId, "고객 ID");
    requireUuid(visitId, "방문 ID");
    if (frameType == null) {
      throw new IllegalArgumentException("프레임 타입은 null일 수 없습니다.");
    }
    return new Myself(
        UUID.randomUUID(),
        customerId,
        visitId,
        frameType,
        requireText(sourceImageObjectKey, "원본 이미지 객체 키"));
  }

  public void complete(String resultImageObjectKey, Instant completedAt) {
    requireProcessing("처리 중인 Myself 작업만 완료할 수 있습니다.");
    this.resultImageObjectKey = requireText(resultImageObjectKey, "결과 이미지 객체 키");
    requireInstant(completedAt, "Myself 완료 시각");
    this.failureCode = null;
    this.completedAt = completedAt;
    this.status = MyselfStatus.COMPLETED;
  }

  public void fail(String failureCode, Instant completedAt) {
    requireProcessing("처리 중인 Myself 작업만 실패 처리할 수 있습니다.");
    this.failureCode = requireText(failureCode, "Myself 실패 코드");
    requireInstant(completedAt, "Myself 실패 시각");
    this.resultImageObjectKey = null;
    this.completedAt = completedAt;
    this.status = MyselfStatus.FAILED;
  }

  public void clearSourceImage() {
    if (status != MyselfStatus.COMPLETED && status != MyselfStatus.FAILED) {
      throw new IllegalStateException("완료되거나 실패한 Myself 작업만 원본을 삭제할 수 있습니다.");
    }
    this.sourceImageObjectKey = null;
  }

  public UUID getId() {
    return id;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public UUID getVisitId() {
    return visitId;
  }

  public FrameType getFrameType() {
    return frameType;
  }

  public MyselfStatus getStatus() {
    return status;
  }

  public String getSourceImageObjectKey() {
    return sourceImageObjectKey;
  }

  public String getResultImageObjectKey() {
    return resultImageObjectKey;
  }

  public String getFailureCode() {
    return failureCode;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  private void requireProcessing(String message) {
    if (status != MyselfStatus.PROCESSING) {
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
