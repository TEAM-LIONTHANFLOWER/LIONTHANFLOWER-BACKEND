// 직원이 Arc와 Visit Memory 생성에 사용할 제품 목록을 조회하는 서비스
package com.lionthanflower.application.staff;

import com.lionthanflower.application.staff.dto.StaffProductResponse;
import com.lionthanflower.domain.product.entity.Product;
import com.lionthanflower.domain.product.entity.ProductVariant;
import com.lionthanflower.infrastructure.persistence.ProductRepository;
import com.lionthanflower.infrastructure.persistence.ProductVariantRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffProductService {

  private final ProductRepository productRepository;
  private final ProductVariantRepository productVariantRepository;

  public StaffProductService(
      ProductRepository productRepository, ProductVariantRepository productVariantRepository) {
    this.productRepository = productRepository;
    this.productVariantRepository = productVariantRepository;
  }

  @Transactional(readOnly = true)
  public List<StaffProductResponse> getProducts() {
    List<Product> products = productRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    if (products.isEmpty()) {
      return List.of();
    }

    List<UUID> productIds = products.stream().map(Product::getId).toList();
    Map<UUID, List<ProductVariant>> variantsByProduct =
        productVariantRepository.findAllByProductIdIn(productIds).stream()
            .collect(Collectors.groupingBy(ProductVariant::getProductId));

    return products.stream()
        .map(
            product ->
                new StaffProductResponse(
                    product.getId(),
                    product.getExternalProductCode(),
                    product.getName(),
                    product.getCategory(),
                    variantsByProduct.getOrDefault(product.getId(), List.of()).stream()
                        .sorted(
                            java.util.Comparator.comparing(ProductVariant::getExternalVariantCode))
                        .map(StaffProductService::toVariantResponse)
                        .toList()))
        .toList();
  }

  private static StaffProductResponse.VariantResponse toVariantResponse(ProductVariant variant) {
    return new StaffProductResponse.VariantResponse(
        variant.getId(), variant.getExternalVariantCode(), variant.getColor(), variant.getOption());
  }
}
