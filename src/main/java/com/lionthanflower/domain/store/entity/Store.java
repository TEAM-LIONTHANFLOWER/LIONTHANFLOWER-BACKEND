// 매장 식별 정보와 QR 진입 코드를 관리하는 엔티티
package com.lionthanflower.domain.store.entity;

import com.lionthanflower.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "stores")
public class Store extends BaseEntity {

  @jakarta.persistence.Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "code", nullable = false, unique = true, length = 100)
  private String code;

  protected Store() {}

  private Store(UUID id, String name, String code) {
    this.id = id;
    this.name = name;
    this.code = code;
  }

  public static Store create(String name, String code) {
    return new Store(UUID.randomUUID(), requireText(name, "매장 이름"), requireText(code, "매장 코드"));
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getCode() {
    return code;
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + "은 비어 있을 수 없습니다.");
    }
    return value.trim();
  }
}
