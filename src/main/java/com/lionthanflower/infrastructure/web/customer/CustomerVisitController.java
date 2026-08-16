// 고객 서비스 진입 HTTP API를 제공하는 Controller
package com.lionthanflower.infrastructure.web.customer;

import com.lionthanflower.application.customer.CustomerVisitService;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import com.lionthanflower.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers/visits")
public class CustomerVisitController {

  private static final String CUSTOMER_TOKEN_COOKIE = "customer_token";

  private final CustomerVisitService service;
  private final boolean cookieSecure;
  private final long cookieMaxAge;

  public CustomerVisitController(
      CustomerVisitService service,
      @Value("${app.customer-session.cookie-secure:false}") boolean cookieSecure,
      @Value("${app.customer-session.cookie-max-age:604800}") long cookieMaxAge) {
    this.service = service;
    this.cookieSecure = cookieSecure;
    this.cookieMaxAge = cookieMaxAge;
  }

  @Operation(summary = "고객 서비스 진입", description = "익명 고객을 식별하거나 생성하고 새로운 ONBOARDING 방문을 생성합니다.")
  @PostMapping
  public ResponseEntity<ApiResponse<EntryResponse>> enter(
      @CookieValue(name = CUSTOMER_TOKEN_COOKIE, required = false) String rawToken) {
    CustomerVisitService.EntryResult result = service.enter(rawToken);
    ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.CREATED);
    if (result.issuedToken() != null) {
      response.header(HttpHeaders.SET_COOKIE, customerCookie(result.issuedToken()).toString());
    }
    return response.body(ApiResponse.success(EntryResponse.from(result)));
  }

  private ResponseCookie customerCookie(String rawToken) {
    return ResponseCookie.from(CUSTOMER_TOKEN_COOKIE, rawToken)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/")
        .maxAge(Duration.ofSeconds(cookieMaxAge))
        .build();
  }

  public record EntryResponse(UUID visitId, String customerName, VisitStatus status) {

    static EntryResponse from(CustomerVisitService.EntryResult result) {
      return new EntryResponse(result.visitId(), result.customerName(), result.status());
    }
  }
}
