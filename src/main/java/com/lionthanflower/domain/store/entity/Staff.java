// 매장에 소속된 개인 기기 직원 프로필을 관리하는 엔티티
package com.lionthanflower.domain.store.entity;

import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.global.entity.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
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

  @Column(name = "token_hash", nullable = false, unique = true, length = 128)
  private String tokenHash;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "staff_languages", joinColumns = @JoinColumn(name = "staff_id"))
  @Enumerated(EnumType.STRING)
  @Column(name = "language", nullable = false, length = 40)
  private Set<LanguageCode> languages = new HashSet<>();

  protected Staff() {}

  private Staff(UUID id, UUID storeId, String name, String tokenHash, Set<LanguageCode> languages) {
    this.id = id;
    this.storeId = storeId;
    this.name = name;
    this.tokenHash = tokenHash;
    this.languages = new HashSet<>(languages);
  }

  public static Staff create(
      UUID storeId, String name, String tokenHash, Set<LanguageCode> languages) {
    if (storeId == null) {
      throw new IllegalArgumentException("매장 ID는 null일 수 없습니다.");
    }
    if (languages == null || languages.isEmpty() || languages.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("직원 구사 언어는 하나 이상이어야 합니다.");
    }
    return new Staff(
        UUID.randomUUID(),
        storeId,
        requireText(name, "직원 이름"),
        requireText(tokenHash, "직원 토큰 해시"),
        Set.copyOf(languages));
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

  public String getTokenHash() {
    return tokenHash;
  }

  public Set<LanguageCode> getLanguages() {
    return Set.copyOf(languages);
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + "은 비어 있을 수 없습니다.");
    }
    return value.trim();
  }
}
