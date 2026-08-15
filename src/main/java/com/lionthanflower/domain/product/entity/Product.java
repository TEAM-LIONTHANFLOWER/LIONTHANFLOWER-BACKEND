// 내부 제품 카탈로그의 제품 종류와 상품명을 관리하는 엔티티
package com.lionthanflower.domain.product.entity;

import com.lionthanflower.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

  @jakarta.persistence.Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @Column(name = "external_product_code", nullable = false, unique = true, length = 100)
  private String externalProductCode;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false, length = 40)
  private ProductCategory category;

  protected Product() {}

  private Product(UUID id, String externalProductCode, String name, ProductCategory category) {
    this.id = id;
    this.externalProductCode = externalProductCode;
    this.name = name;
    this.category = category;
  }

  public static Product create(String externalProductCode, String name, ProductCategory category) {
    if (category == null) {
      throw new IllegalArgumentException("제품 카테고리는 null일 수 없습니다.");
    }
    return new Product(
        UUID.randomUUID(),
        requireText(externalProductCode, "외부 제품 코드"),
        requireText(name, "제품명"),
        category);
  }

  public UUID getId() {
    return id;
  }

  public String getExternalProductCode() {
    return externalProductCode;
  }

  public String getName() {
    return name;
  }

  public ProductCategory getCategory() {
    return category;
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + "은 비어 있을 수 없습니다.");
    }
    return value.trim();
  }
}
