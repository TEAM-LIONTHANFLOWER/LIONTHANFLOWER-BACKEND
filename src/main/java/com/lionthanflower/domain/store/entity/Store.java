// 매장 식별 정보와 QR 진입 코드를 관리하는 엔티티
package com.lionthanflower.domain.store.entity;

import com.lionthanflower.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "stores")
public class Store extends BaseEntity {

  private static final Set<String> ISO_COUNTRY_CODES = Set.of(Locale.getISOCountries());

  @jakarta.persistence.Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "code", nullable = false, unique = true, length = 100)
  private String code;

  @Column(name = "country_code", nullable = false, length = 2)
  private String countryCode;

  protected Store() {}

  private Store(UUID id, String name, String code, String countryCode) {
    this.id = id;
    this.name = name;
    this.code = code;
    this.countryCode = countryCode;
  }

  public static Store create(String name, String code, String countryCode) {
    String normalizedCountryCode = requireText(countryCode, "매장 국가 코드").toUpperCase(Locale.ROOT);
    if (!ISO_COUNTRY_CODES.contains(normalizedCountryCode)) {
      throw new IllegalArgumentException("매장 국가 코드는 ISO alpha-2 형식이어야 합니다.");
    }
    return new Store(
        UUID.randomUUID(),
        requireText(name, "매장 이름"),
        requireText(code, "매장 코드"),
        normalizedCountryCode);
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

  public String getCountryCode() {
    return countryCode;
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + "은 비어 있을 수 없습니다.");
    }
    return value.trim();
  }
}
