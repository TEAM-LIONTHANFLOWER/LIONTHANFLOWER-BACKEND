// 제품별 정확한 컬러와 사이즈 판매 단위를 관리하는 엔티티
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
@Table(name = "product_variants")
public class ProductVariant extends BaseEntity {

  @jakarta.persistence.Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "product_id", nullable = false, length = 36)
  private UUID productId;

  @Column(name = "external_variant_code", nullable = false, unique = true, length = 100)
  private String externalVariantCode;

  @Column(name = "image_object_key", nullable = false, length = 1024)
  private String imageObjectKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "color", nullable = false, length = 40)
  private ProductColor color;

  @Enumerated(EnumType.STRING)
  @Column(name = "size_option", nullable = false, length = 40)
  private ProductOption option;

  protected ProductVariant() {}

  private ProductVariant(
      UUID id,
      UUID productId,
      String externalVariantCode,
      String imageObjectKey,
      ProductColor color,
      ProductOption option) {
    this.id = id;
    this.productId = productId;
    this.externalVariantCode = externalVariantCode;
    this.imageObjectKey = imageObjectKey;
    this.color = color;
    this.option = option;
  }

  public static ProductVariant create(
      UUID productId,
      String externalVariantCode,
      String imageObjectKey,
      ProductColor color,
      ProductOption option) {
    requireUuid(productId, "제품 ID");
    if (color == null) {
      throw new IllegalArgumentException("제품 컬러는 null일 수 없습니다.");
    }
    if (option == null) {
      throw new IllegalArgumentException("제품 옵션은 null일 수 없습니다.");
    }
    return new ProductVariant(
        UUID.randomUUID(),
        productId,
        requireText(externalVariantCode, "외부 Variant 코드"),
        requireText(imageObjectKey, "제품 이미지 객체 키"),
        color,
        option);
  }

  public UUID getId() {
    return id;
  }

  public UUID getProductId() {
    return productId;
  }

  public String getExternalVariantCode() {
    return externalVariantCode;
  }

  public String getImageObjectKey() {
    return imageObjectKey;
  }

  public ProductColor getColor() {
    return color;
  }

  public ProductOption getOption() {
    return option;
  }

  private static void requireUuid(UUID value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(fieldName + "는 null일 수 없습니다.");
    }
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + "은 비어 있을 수 없습니다.");
    }
    return value.trim();
  }
}
