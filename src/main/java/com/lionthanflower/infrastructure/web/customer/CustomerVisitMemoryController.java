// 고객이 알림에서 진입하는 Visit Memory 상세 내용을 제공하는 HTTP Controller
package com.lionthanflower.infrastructure.web.customer;

import com.lionthanflower.application.customer.CustomerVisitMemoryQueryService;
import com.lionthanflower.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers/visit-memories")
public class CustomerVisitMemoryController {

  private static final String CUSTOMER_TOKEN_COOKIE = "customer_token";

  private final CustomerVisitMemoryQueryService service;

  public CustomerVisitMemoryController(CustomerVisitMemoryQueryService service) {
    this.service = service;
  }

  @Operation(summary = "고객 Visit Memory 상세 조회", description = "고객 본인의 최종 저장된 Visit Memory를 조회합니다.")
  @GetMapping("/{visitMemoryId}")
  public ApiResponse<CustomerVisitMemoryQueryService.VisitMemoryDetail> getMemory(
      @PathVariable UUID visitMemoryId,
      @CookieValue(name = CUSTOMER_TOKEN_COOKIE, required = false) String rawToken) {
    return ApiResponse.success(service.getMemory(visitMemoryId, rawToken));
  }
}
