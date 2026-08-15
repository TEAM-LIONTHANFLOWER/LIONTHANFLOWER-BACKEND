// 제품 카탈로그와 판매 Variant 생성 규칙을 검증하는 테스트
package com.lionthanflower.domain.product.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductDomainTest {

  @Test
  void 제품과_판매_Variant를_생성한다() {
    Product product = Product.create("MCM-BAG-001", "Stark Backpack", ProductCategory.BAG);
    ProductVariant variant =
        ProductVariant.create(
            product.getId(),
            "MCM-BAG-001-BLACK-M",
            "products/stark-black-m.png",
            ProductColor.BLACK,
            ProductOption.M);

    assertThat(product.getCategory()).isEqualTo(ProductCategory.BAG);
    assertThat(variant.getProductId()).isEqualTo(product.getId());
    assertThat(variant.getColor()).isEqualTo(ProductColor.BLACK);
    assertThat(variant.getOption()).isEqualTo(ProductOption.M);
  }

  @Test
  void Variant는_제품_ID가_필요하다() {
    assertThatThrownBy(
            () ->
                ProductVariant.create(
                    null, "VARIANT-001", "products/001.png", ProductColor.WHITE, ProductOption.S))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("제품 ID는 null일 수 없습니다.");
  }

  @Test
  void 제품과_Variant의_필수_선택값을_검증한다() {
    assertThatThrownBy(() -> Product.create("MCM-BAG-001", "Stark Backpack", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("제품 카테고리는 null일 수 없습니다.");

    assertThatThrownBy(
            () ->
                ProductVariant.create(
                    UUID.randomUUID(), "VARIANT-001", "products/001.png", null, ProductOption.S))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("제품 컬러는 null일 수 없습니다.");

    assertThatThrownBy(
            () ->
                ProductVariant.create(
                    UUID.randomUUID(), "VARIANT-001", "products/001.png", ProductColor.WHITE, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("제품 옵션은 null일 수 없습니다.");
  }
}
