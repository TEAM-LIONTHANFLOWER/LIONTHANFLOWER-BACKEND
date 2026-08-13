// 매장에 소속된 사전 등록 직원 프로필을 관리하는 엔티티
package com.lionthanflower.domain.store.entity;

import com.lionthanflower.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "staff")
public class Staff extends BaseEntity {

  @jakarta.persistence.Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "store_id", nullable = false, length = 36)
  private UUID storeId;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "profile_image_url", length = 2048)
  private String profileImageUrl;

  @Column(name = "active", nullable = false)
  private boolean active;

  protected Staff() {}

  private Staff(UUID id, UUID storeId, String name, String profileImageUrl) {
    this.id = id;
    this.storeId = storeId;
    this.name = name;
    this.profileImageUrl = profileImageUrl;
    this.active = true;
  }

  public static Staff create(UUID storeId, String name, String profileImageUrl) {
    if (storeId == null) {
      throw new IllegalArgumentException("매장 ID는 null일 수 없습니다.");
    }
    return new Staff(UUID.randomUUID(), storeId, requireText(name, "직원 이름"), profileImageUrl);
  }

  public UUID getId() {
    return id;
  }

  public UUID getStoreId() {
    return storeId;
  }

  public String getName() {
    return name;
  }

  public String getProfileImageUrl() {
    return profileImageUrl;
  }

  public boolean isActive() {
    return active;
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + "은 비어 있을 수 없습니다.");
    }
    return value.trim();
  }
}
