// 고객에게 표시할 웹 알림과 읽음 상태를 관리하는 엔티티
package com.lionthanflower.domain.notification.entity;

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
    name = "customer_notifications",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_customer_notifications_type_resource",
            columnNames = {"customer_id", "type", "resource_id"}),
    indexes =
        @Index(
            name = "idx_customer_notifications_customer_created_at",
            columnList = "customer_id, created_at"))
public class CustomerNotification extends BaseEntity {

  @jakarta.persistence.Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "customer_id", nullable = false, length = 36)
  private UUID customerId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 40)
  private CustomerNotificationType type;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "resource_id", nullable = false, length = 36)
  private UUID resourceId;

  @Column(name = "message", nullable = false, length = 255)
  private String message;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "read_at")
  private Instant readAt;

  protected CustomerNotification() {}

  private CustomerNotification(
      UUID id, UUID customerId, CustomerNotificationType type, UUID resourceId, String message) {
    this.id = id;
    this.customerId = customerId;
    this.type = type;
    this.resourceId = resourceId;
    this.message = message;
  }

  public static CustomerNotification createVisitMemory(
      UUID customerId, UUID visitMemoryId, String message) {
    return new CustomerNotification(
        UUID.randomUUID(),
        requireUuid(customerId, "고객 ID"),
        CustomerNotificationType.VISIT_MEMORY,
        requireUuid(visitMemoryId, "Visit Memory ID"),
        requireText(message, "알림 메시지"));
  }

  public void markRead(Instant readAt) {
    if (this.readAt != null) {
      return;
    }
    if (readAt == null) {
      throw new IllegalArgumentException("알림 읽음 시각은 null일 수 없습니다.");
    }
    this.readAt = readAt;
  }

  public UUID getId() {
    return id;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public CustomerNotificationType getType() {
    return type;
  }

  public UUID getResourceId() {
    return resourceId;
  }

  public String getMessage() {
    return message;
  }

  public Instant getReadAt() {
    return readAt;
  }

  public boolean isRead() {
    return readAt != null;
  }

  private static UUID requireUuid(UUID value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(fieldName + "는 null일 수 없습니다.");
    }
    return value;
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + "은 비어 있을 수 없습니다.");
    }
    return value.trim();
  }
}
