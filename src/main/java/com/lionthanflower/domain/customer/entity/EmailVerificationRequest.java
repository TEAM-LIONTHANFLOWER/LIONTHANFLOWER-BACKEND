// 이메일 인증번호 요청의 검증 정보와 만료 및 사용 상태를 저장하는 엔티티
package com.lionthanflower.domain.customer.entity;

import com.lionthanflower.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "email_verification_requests")
public class EmailVerificationRequest extends BaseEntity {
  @JdbcTypeCode(SqlTypes.CHAR)
  @jakarta.persistence.Id
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "customer_id", nullable = false, length = 36)
  private UUID customerId;

  @Column(name = "email", nullable = false, length = 255)
  private String email;

  @Column(name = "purpose", nullable = false, length = 40)
  private String purpose;

  @Column(name = "code_hash", nullable = false, length = 128)
  private String codeHash;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "failed_attempts", nullable = false)
  private int failedAttempts;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "consumed_at")
  private Instant consumedAt;

  @JdbcTypeCode(SqlTypes.TIMESTAMP)
  @Column(name = "invalidated_at")
  private Instant invalidatedAt;

  protected EmailVerificationRequest() {}

  @PrePersist
  @PreUpdate
  private void validateEmail() {
    EmailAddress.validate(email);
  }

  public static EmailVerificationRequest create(
      UUID customerId, String email, String purpose, String codeHash, Instant expiresAt) {
    if (customerId == null
        || purpose == null
        || purpose.isBlank()
        || purpose.length() > 40
        || codeHash == null
        || codeHash.isBlank()
        || codeHash.length() > 128
        || expiresAt == null) {
      throw new IllegalArgumentException("이메일 인증 요청의 필수 정보가 올바르지 않습니다.");
    }
    EmailVerificationRequest request = new EmailVerificationRequest();
    request.id = UUID.randomUUID();
    request.customerId = customerId;
    request.email = EmailAddress.validate(email);
    request.purpose = purpose;
    request.codeHash = codeHash;
    request.expiresAt = expiresAt;
    return request;
  }

  public UUID getId() {
    return id;
  }

  public UUID getCustomerId() {
    return customerId;
  }

  public String getEmail() {
    return email;
  }

  public String getPurpose() {
    return purpose;
  }

  public String getCodeHash() {
    return codeHash;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public int getFailedAttempts() {
    return failedAttempts;
  }

  public Instant getConsumedAt() {
    return consumedAt;
  }

  public Instant getInvalidatedAt() {
    return invalidatedAt;
  }
}
