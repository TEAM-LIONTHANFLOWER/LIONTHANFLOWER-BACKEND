// 고객 방문의 온보딩, 매칭, Arc 진행과 종료 상태를 관리하는 엔티티
package com.lionthanflower.domain.visit.entity;

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
    name = "visits",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_visits_store_waiting_number",
            columnNames = {"store_id", "waiting_number"}),
    indexes = {
      @Index(
          name = "idx_visits_store_status_created_at",
          columnList = "store_id, status, created_at"),
      @Index(name = "idx_visits_staff_status", columnList = "staff_id, status")
    })
public class Visit extends BaseEntity {

  @jakarta.persistence.Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "customer_id", nullable = false, length = 36)
  private UUID customerId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "store_id", nullable = false, length = 36)
  private UUID storeId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "staff_id", length = 36)
  private UUID staffId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "store_device_id", length = 36)
  private UUID storeDeviceId;

  @Column(name = "waiting_number", nullable = false, length = 40)
  private String waitingNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "service_language", length = 40)
  private ServiceLanguage serviceLanguage;

  @Enumerated(EnumType.STRING)
  @Column(name = "interaction_style", length = 40)
  private InteractionStyle interactionStyle;

  @Column(name = "additional_request", length = 1000)
  private String additionalRequest;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 40)
  private VisitStatus status;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "arc_creation_granted_at")
  private Instant arcCreationGrantedAt;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "matched_at")
  private Instant matchedAt;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "completed_at")
  private Instant completedAt;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "canceled_at")
  private Instant canceledAt;

  @jakarta.persistence.Version
  @Column(name = "version", nullable = false)
  private long version;

  protected Visit() {}

  private Visit(UUID id, UUID customerId, UUID storeId, String waitingNumber) {
    this.id = id;
    this.customerId = customerId;
    this.storeId = storeId;
    this.waitingNumber = waitingNumber;
    this.status = VisitStatus.ONBOARDING;
  }

  public static Visit create(UUID customerId, UUID storeId, String waitingNumber) {
    requireUuid(customerId, "고객 ID");
    requireUuid(storeId, "매장 ID");
    return new Visit(UUID.randomUUID(), customerId, storeId, requireText(waitingNumber, "대기번호"));
  }

  public void completeOnboarding(
      ServiceLanguage serviceLanguage,
      InteractionStyle interactionStyle,
      String additionalRequest) {
    requireStatus(VisitStatus.ONBOARDING, "온보딩 중인 방문만 완료할 수 있습니다.");
    if (serviceLanguage == null) {
      throw new IllegalArgumentException("서비스 이용 언어는 null일 수 없습니다.");
    }
    if (interactionStyle == null) {
      throw new IllegalArgumentException("직원 응대 방식은 null일 수 없습니다.");
    }
    this.serviceLanguage = serviceLanguage;
    this.interactionStyle = interactionStyle;
    this.additionalRequest = normalizeNullable(additionalRequest);
    this.status =
        interactionStyle == InteractionStyle.STAFF_RECOMMENDATION
            ? VisitStatus.WAITING_FOR_STAFF
            : VisitStatus.SELF_GUIDED;
  }

  public void matchForRecommendation(UUID staffId, UUID storeDeviceId, Instant matchedAt) {
    requireStatus(VisitStatus.WAITING_FOR_STAFF, "직원 추천 대기 중인 방문만 매칭할 수 있습니다.");
    requireUuid(staffId, "직원 ID");
    requireUuid(storeDeviceId, "단말 ID");
    requireInstant(matchedAt, "매칭 시각");
    this.staffId = staffId;
    this.storeDeviceId = storeDeviceId;
    this.matchedAt = matchedAt;
    this.status = VisitStatus.MATCHED;
  }

  public void startArcForSelfGuided(UUID staffId, UUID storeDeviceId, Instant grantedAt) {
    requireStatus(VisitStatus.SELF_GUIDED, "혼자 보기 상태인 방문만 직원 연결을 시작할 수 있습니다.");
    requireUuid(staffId, "직원 ID");
    requireUuid(storeDeviceId, "단말 ID");
    requireInstant(grantedAt, "Arc 생성 권한 부여 시각");
    this.staffId = staffId;
    this.storeDeviceId = storeDeviceId;
    this.matchedAt = grantedAt;
    this.arcCreationGrantedAt = grantedAt;
    this.status = VisitStatus.ARC_IN_PROGRESS;
  }

  public void grantArcCreation(Instant grantedAt) {
    requireStatus(VisitStatus.MATCHED, "매칭된 방문만 Arc 생성 권한을 받을 수 있습니다.");
    requireInstant(grantedAt, "Arc 생성 권한 부여 시각");
    this.arcCreationGrantedAt = grantedAt;
    this.status = VisitStatus.ARC_IN_PROGRESS;
  }

  public boolean isArcCreationGranted() {
    return arcCreationGrantedAt != null;
  }

  public void completeWithoutPurchase(Instant completedAt) {
    requireStatus(VisitStatus.SELF_GUIDED, "혼자 보기 상태인 방문만 구매 없이 종료할 수 있습니다.");
    requireInstant(completedAt, "방문 종료 시각");
    this.completedAt = completedAt;
    this.status = VisitStatus.COMPLETED;
  }

  public void complete(Instant completedAt) {
    requireStatus(VisitStatus.ARC_IN_PROGRESS, "Arc 진행 중인 방문만 종료할 수 있습니다.");
    requireInstant(completedAt, "방문 종료 시각");
    this.completedAt = completedAt;
    this.status = VisitStatus.COMPLETED;
  }

  public void cancel(Instant canceledAt) {
    if (status == VisitStatus.COMPLETED || status == VisitStatus.CANCELED) {
      throw new IllegalStateException("종료된 방문은 취소할 수 없습니다.");
    }
    requireInstant(canceledAt, "방문 취소 시각");
    this.canceledAt = canceledAt;
    this.status = VisitStatus.CANCELED;
  }

  public UUID getId() {
    return id;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public UUID getStoreId() {
    return storeId;
  }

  public UUID getStaffId() {
    return staffId;
  }

  public UUID getStoreDeviceId() {
    return storeDeviceId;
  }

  public String getWaitingNumber() {
    return waitingNumber;
  }

  public ServiceLanguage getServiceLanguage() {
    return serviceLanguage;
  }

  public InteractionStyle getInteractionStyle() {
    return interactionStyle;
  }

  public String getAdditionalRequest() {
    return additionalRequest;
  }

  public VisitStatus getStatus() {
    return status;
  }

  public Instant getArcCreationGrantedAt() {
    return arcCreationGrantedAt;
  }

  public Instant getMatchedAt() {
    return matchedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public Instant getCanceledAt() {
    return canceledAt;
  }

  public long getVersion() {
    return version;
  }

  private void requireStatus(VisitStatus expected, String message) {
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

  private static String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
