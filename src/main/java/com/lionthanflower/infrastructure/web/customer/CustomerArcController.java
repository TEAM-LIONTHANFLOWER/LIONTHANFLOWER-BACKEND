// 고객이 보유한 Arc 목록과 상세 내용을 제공하는 HTTP Controller
package com.lionthanflower.infrastructure.web.customer;

import com.lionthanflower.application.customer.CustomerArcQueryService;
import com.lionthanflower.domain.arc.entity.ArcStatus;
import com.lionthanflower.domain.product.entity.ProductColor;
import com.lionthanflower.domain.product.entity.ProductOption;
import com.lionthanflower.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers/arcs")
public class CustomerArcController {

  private static final String CUSTOMER_TOKEN_COOKIE = "customer_token";

  private final CustomerArcQueryService service;

  public CustomerArcController(CustomerArcQueryService service) {
    this.service = service;
  }

  @Operation(
      summary = "고객 Arc 목록 조회",
      description = "고객에게 공유되거나 최종 저장된 Arc의 매장 이름과 편지 본문을 최신순으로 조회합니다.")
  @GetMapping
  public ApiResponse<List<ArcListItemResponse>> getArcs(
      @CookieValue(name = CUSTOMER_TOKEN_COOKIE, required = false) String rawToken) {
    return ApiResponse.success(
        service.getArcs(rawToken).stream().map(ArcListItemResponse::from).toList());
  }

  @Operation(summary = "고객 Arc 상세 조회", description = "고객 본인의 공개 Arc와 전체 구매 제품을 조회합니다.")
  @GetMapping("/{arcId}")
  public ApiResponse<ArcDetailResponse> getArc(
      @PathVariable UUID arcId,
      @CookieValue(name = CUSTOMER_TOKEN_COOKIE, required = false) String rawToken) {
    return ApiResponse.success(ArcDetailResponse.from(service.getArc(arcId, rawToken)));
  }

  public record ProductResponse(
      UUID productVariantId, String productName, ProductColor color, ProductOption option) {

    static ProductResponse from(CustomerArcQueryService.ProductView product) {
      return new ProductResponse(
          product.productVariantId(), product.productName(), product.color(), product.option());
    }
  }

  public record ArcListItemResponse(
      UUID arcId,
      int arcNumber,
      String storeName,
      String momentSummary,
      String momentToRemember,
      ProductResponse representativeProduct,
      ArcStatus status,
      Instant sharedAt,
      Instant finalizedAt) {

    static ArcListItemResponse from(CustomerArcQueryService.ArcSummary arc) {
      return new ArcListItemResponse(
          arc.arcId(),
          arc.arcNumber(),
          arc.storeName(),
          arc.momentSummary(),
          arc.momentToRemember(),
          ProductResponse.from(arc.representativeProduct()),
          arc.status(),
          arc.sharedAt(),
          arc.finalizedAt());
    }
  }

  public record ArcDetailResponse(
      UUID arcId,
      int arcNumber,
      String customerName,
      String storeName,
      String countryCode,
      ArcStatus status,
      Instant sharedAt,
      Instant finalizedAt,
      String momentSummary,
      List<String> preferences,
      String momentToRemember,
      List<ProductResponse> purchasedProducts) {

    static ArcDetailResponse from(CustomerArcQueryService.ArcDetail arc) {
      return new ArcDetailResponse(
          arc.arcId(),
          arc.arcNumber(),
          arc.customerName(),
          arc.storeName(),
          arc.countryCode(),
          arc.status(),
          arc.sharedAt(),
          arc.finalizedAt(),
          arc.momentSummary(),
          arc.preferences(),
          arc.momentToRemember(),
          arc.purchasedProducts().stream().map(ProductResponse::from).toList());
    }
  }
}
