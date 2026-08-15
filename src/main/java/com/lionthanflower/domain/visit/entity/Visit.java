// 고객 방문의 온보딩, 직원 연결, 구매 판단과 종료 상태를 관리하는 엔티티
package com.lionthanflower.domain.visit.entity;

import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "visits",
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

  @Enumerated(EnumType.STRING)
  @Column(name = "service_language", length = 40)
  private LanguageCode serviceLanguage;

  @Enumerated(EnumType.STRING)
  @Column(name = "interaction_style", length = 40)
  private InteractionStyle interactionStyle;

  @Column(name = "additional_request", length = 1000)
  private String additionalRequest;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 40)
  private VisitStatus status;

  @Enumerated(EnumType.STRING)
  @Column(name = "purchase_decision", nullable = false, length = 40)
  private PurchaseDecision purchaseDecision;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "purchase_decided_by_staff_id", length = 36)
  private UUID purchaseDecidedByStaffId;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "purchase_decided_at")
  private Instant purchaseDecidedAt;

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

  private Visit(UUID id, UUID customerId, UUID storeId) {
    this.id = id;
    this.customerId = customerId;
    this.storeId = storeId;
    this.status = VisitStatus.ONBOARDING;
    this.purchaseDecision = PurchaseDecision.PENDING;
  }

  public static Visit create(UUID customerId, UUID storeId) {
    requireUuid(customerId, "고객 ID");
    requireUuid(storeId, "매장 ID");
    return new Visit(UUID.randomUUID(), customerId, storeId);
  }

  public void completeOnboarding(
      LanguageCode serviceLanguage, InteractionStyle interactionStyle, String additionalRequest) {
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
            : VisitStatus.ACTIVE;
  }

  public void assignStaff(UUID staffId, Instant matchedAt) {
    if (status != VisitStatus.WAITING_FOR_STAFF
        && !(status == VisitStatus.ACTIVE && this.staffId == null)) {
      throw new IllegalStateException("직원을 연결할 수 있는 방문 상태가 아닙니다.");
    }
    requireUuid(staffId, "직원 ID");
    requireInstant(matchedAt, "매칭 시각");
    this.staffId = staffId;
    this.matchedAt = matchedAt;
    this.status = VisitStatus.ACTIVE;
  }

  public void confirmPurchase(UUID staffId, Instant decidedAt) {
    decidePurchase(staffId, PurchaseDecision.PURCHASED, VisitStatus.ARC_IN_PROGRESS, decidedAt);
  }

  public void confirmNoPurchase(UUID staffId, Instant decidedAt) {
    decidePurchase(
        staffId, PurchaseDecision.NOT_PURCHASED, VisitStatus.VISIT_MEMORY_IN_PROGRESS, decidedAt);
  }

  public void complete(Instant completedAt) {
    if (status != VisitStatus.ARC_IN_PROGRESS && status != VisitStatus.VISIT_MEMORY_IN_PROGRESS) {
      throw new IllegalStateException("Arc 또는 Visit Memory 진행 중인 방문만 종료할 수 있습니다.");
    }
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

  public LanguageCode getServiceLanguage() {
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

  public PurchaseDecision getPurchaseDecision() {
    return purchaseDecision;
  }

  public UUID getPurchaseDecidedByStaffId() {
    return purchaseDecidedByStaffId;
  }

  public Instant getPurchaseDecidedAt() {
    return purchaseDecidedAt;
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

  private void decidePurchase(
      UUID staffId, PurchaseDecision decision, VisitStatus nextStatus, Instant decidedAt) {
    requireStatus(VisitStatus.ACTIVE, "진행 중인 방문만 구매 여부를 확정할 수 있습니다.");
    if (!Objects.equals(this.staffId, staffId)) {
      throw new IllegalArgumentException("담당 직원만 구매 여부를 확정할 수 있습니다.");
    }
    requireUuid(staffId, "직원 ID");
    requireInstant(decidedAt, "구매 판단 시각");
    this.purchaseDecision = decision;
    this.purchaseDecidedByStaffId = staffId;
    this.purchaseDecidedAt = decidedAt;
    this.status = nextStatus;
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

  private static String normalizeNullable(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
