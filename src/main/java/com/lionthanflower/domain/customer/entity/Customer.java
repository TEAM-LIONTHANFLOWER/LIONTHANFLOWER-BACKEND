// 익명 식별 토큰과 온보딩 고객 이름을 관리하는 엔티티
package com.lionthanflower.domain.customer.entity;

import com.lionthanflower.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "customers")
public class Customer extends BaseEntity {

  @jakarta.persistence.Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @Column(name = "name", length = 100)
  private String name;

  @Column(name = "token_hash", nullable = false, unique = true, length = 128)
  private String tokenHash;

  protected Customer() {}

  private Customer(UUID id, String tokenHash) {
    this.id = id;
    this.tokenHash = tokenHash;
  }

  public static Customer create(String tokenHash) {
    return new Customer(UUID.randomUUID(), requireText(tokenHash, "고객 토큰 해시"));
  }

  public void updateName(String name) {
    this.name = requireText(name, "고객 이름");
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + "은 비어 있을 수 없습니다.");
    }
    return value.trim();
  }
}
