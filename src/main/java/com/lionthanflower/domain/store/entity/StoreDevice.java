// 직원용 매장 단말과 현재 선택 직원을 관리하는 엔티티
package com.lionthanflower.domain.store.entity;

import com.lionthanflower.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "store_devices")
public class StoreDevice extends BaseEntity {

  @jakarta.persistence.Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "store_id", nullable = false, length = 36)
  private UUID storeId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "selected_staff_id", length = 36)
  private UUID selectedStaffId;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "token_hash", nullable = false, unique = true, length = 128)
  private String tokenHash;

  @Column(name = "active", nullable = false)
  private boolean active;

  protected StoreDevice() {}

  private StoreDevice(UUID id, UUID storeId, String name, String tokenHash) {
    this.id = id;
    this.storeId = storeId;
    this.name = name;
    this.tokenHash = tokenHash;
    this.active = true;
  }

  public static StoreDevice create(UUID storeId, String name, String tokenHash) {
    if (storeId == null) {
      throw new IllegalArgumentException("매장 ID는 null일 수 없습니다.");
    }
    return new StoreDevice(
        UUID.randomUUID(), storeId, requireText(name, "단말 이름"), requireText(tokenHash, "단말 토큰 해시"));
  }

  public void selectStaff(UUID staffId) {
    if (!active) {
      throw new IllegalStateException("비활성화된 단말에서는 직원을 선택할 수 없습니다.");
    }
    if (staffId == null) {
      throw new IllegalArgumentException("직원 ID는 null일 수 없습니다.");
    }
    this.selectedStaffId = staffId;
  }

  public void deactivate() {
    this.active = false;
  }

  public UUID getId() {
    return id;
  }

  public UUID getStoreId() {
    return storeId;
  }

  public UUID getSelectedStaffId() {
    return selectedStaffId;
  }

  public String getName() {
    return name;
  }

  public String getTokenHash() {
    return tokenHash;
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
