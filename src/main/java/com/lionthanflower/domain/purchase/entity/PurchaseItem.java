// 구매에 포함된 제품 Variant 항목을 관리하는 엔티티
package com.lionthanflower.domain.purchase.entity;

import com.lionthanflower.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "purchase_items",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_purchase_items_purchase_variant",
            columnNames = {"purchase_id", "product_variant_id"}))
public class PurchaseItem extends BaseEntity {

  @jakarta.persistence.Id
  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "id", nullable = false, updatable = false, length = 36)
  private UUID id;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "purchase_id", nullable = false, length = 36)
  private UUID purchaseId;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "product_variant_id", nullable = false, length = 36)
  private UUID productVariantId;

  protected PurchaseItem() {}

  private PurchaseItem(UUID id, UUID purchaseId, UUID productVariantId) {
    this.id = id;
    this.purchaseId = purchaseId;
    this.productVariantId = productVariantId;
  }

  public static PurchaseItem create(UUID purchaseId, UUID productVariantId) {
    requireUuid(purchaseId, "구매 ID");
    requireUuid(productVariantId, "제품 Variant ID");
    return new PurchaseItem(UUID.randomUUID(), purchaseId, productVariantId);
  }

  public static List<PurchaseItem> createAll(UUID purchaseId, Collection<UUID> productVariantIds) {
    requireUuid(purchaseId, "구매 ID");
    if (productVariantIds == null || productVariantIds.isEmpty()) {
      throw new IllegalArgumentException("구매 제품은 하나 이상이어야 합니다.");
    }
    if (productVariantIds.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("제품 Variant ID는 null일 수 없습니다.");
    }
    if (new HashSet<>(productVariantIds).size() != productVariantIds.size()) {
      throw new IllegalArgumentException("같은 제품 Variant를 중복 선택할 수 없습니다.");
    }
    return productVariantIds.stream()
        .map(productVariantId -> PurchaseItem.create(purchaseId, productVariantId))
        .toList();
  }

  public UUID getId() {
    return id;
  }

  public UUID getPurchaseId() {
    return purchaseId;
  }

  public UUID getProductVariantId() {
    return productVariantId;
  }

  private static void requireUuid(UUID value, String fieldName) {
    if (value == null) {
      throw new IllegalArgumentException(fieldName + "는 null일 수 없습니다.");
    }
  }
}
