// 직원 제품 목록 조회 서비스의 제품과 Variant 조합을 검증하는 테스트
package com.lionthanflower.application.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.lionthanflower.domain.product.entity.Product;
import com.lionthanflower.domain.product.entity.ProductCategory;
import com.lionthanflower.domain.product.entity.ProductColor;
import com.lionthanflower.domain.product.entity.ProductOption;
import com.lionthanflower.domain.product.entity.ProductVariant;
import com.lionthanflower.infrastructure.persistence.ProductRepository;
import com.lionthanflower.infrastructure.persistence.ProductVariantRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class StaffProductServiceTest {

  @Mock private ProductRepository productRepository;
  @Mock private ProductVariantRepository productVariantRepository;

  private StaffProductService service;

  @BeforeEach
  void setUp() {
    service = new StaffProductService(productRepository, productVariantRepository);
  }

  @Test
  void 제품과_제품별_Variant를_함께_조회한다() {
    Product product = Product.create("MCM-BAG-001", "Stark Backpack", ProductCategory.BAG);
    ProductVariant variant =
        ProductVariant.create(
            product.getId(), "MCM-BAG-001-BLACK-M", ProductColor.BLACK, ProductOption.M);
    when(productRepository.findAll(any(Sort.class))).thenReturn(List.of(product));
    when(productVariantRepository.findAllByProductIdIn(List.of(product.getId())))
        .thenReturn(List.of(variant));

    var result = service.getProducts();

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().productId()).isEqualTo(product.getId());
    assertThat(result.getFirst().name()).isEqualTo("Stark Backpack");
    assertThat(result.getFirst().variants())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.productVariantId()).isEqualTo(variant.getId());
              assertThat(item.color()).isEqualTo(ProductColor.BLACK);
              assertThat(item.option()).isEqualTo(ProductOption.M);
            });
  }
}
