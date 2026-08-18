// 고객 웹 알림 목록과 읽음 처리를 제공하는 HTTP Controller
package com.lionthanflower.infrastructure.web.customer;

import com.lionthanflower.application.customer.CustomerNotificationService;
import com.lionthanflower.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers/notifications")
public class CustomerNotificationController {

  private static final String CUSTOMER_TOKEN_COOKIE = "customer_token";

  private final CustomerNotificationService service;

  public CustomerNotificationController(CustomerNotificationService service) {
    this.service = service;
  }

  @Operation(summary = "고객 알림 목록 조회", description = "고객 본인의 읽음·안 읽음 알림을 최신순으로 조회합니다.")
  @GetMapping
  public ApiResponse<List<CustomerNotificationService.NotificationView>> getNotifications(
      @CookieValue(name = CUSTOMER_TOKEN_COOKIE, required = false) String rawToken) {
    return ApiResponse.success(service.getNotifications(rawToken));
  }

  @Operation(summary = "고객 알림 읽음 처리", description = "고객 본인의 알림을 읽음 상태로 변경합니다.")
  @PatchMapping("/{notificationId}/read")
  public ApiResponse<CustomerNotificationService.NotificationView> markRead(
      @PathVariable UUID notificationId,
      @CookieValue(name = CUSTOMER_TOKEN_COOKIE, required = false) String rawToken) {
    return ApiResponse.success(service.markRead(notificationId, rawToken));
  }
}
