// 직원 제품 목록 조회 API의 제품과 Variant 응답을 정의하는 DTO
package com.lionthanflower.application.staff.dto;

import com.lionthanflower.domain.product.entity.ProductCategory;
import com.lionthanflower.domain.product.entity.ProductColor;
import com.lionthanflower.domain.product.entity.ProductOption;
import java.util.List;
import java.util.UUID;

public record StaffProductResponse(
    UUID productId,
    String externalProductCode,
    String name,
    ProductCategory category,
    List<VariantResponse> variants) {

  public record VariantResponse(
      UUID productVariantId,
      String externalVariantCode,
      ProductColor color,
      ProductOption option) {}
}
