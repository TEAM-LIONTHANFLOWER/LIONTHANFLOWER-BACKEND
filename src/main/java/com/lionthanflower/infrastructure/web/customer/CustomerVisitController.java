// 고객 서비스 진입과 온보딩 진행 HTTP API를 제공하는 Controller
package com.lionthanflower.infrastructure.web.customer;

import com.lionthanflower.application.customer.CustomerVisitService;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.visit.entity.InteractionStyle;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import com.lionthanflower.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

  @Operation(summary = "고객 온보딩 진행", description = "고객의 온보딩 정보를 저장하고 방문 상태를 서비스 진행 상태로 전환합니다.")
  @PatchMapping(value = "/{visitId}/onboarding", consumes = MediaType.APPLICATION_JSON_VALUE)
  public ApiResponse<OnboardingResponse> progressOnboarding(
      @PathVariable UUID visitId,
      @CookieValue(name = CUSTOMER_TOKEN_COOKIE, required = false) String rawToken,
      @Valid @RequestBody OnboardingRequest request) {
    CustomerVisitService.OnboardingResult result =
        service.progressOnboarding(visitId, rawToken, request.toCommand());
    return ApiResponse.success(OnboardingResponse.from(result));
  }

  @Operation(summary = "고객 매칭 상태 조회", description = "직원 추천을 선택한 고객이 담당 직원 배정 상태를 조회합니다.")
  @GetMapping("/{visitId}/matching")
  public ApiResponse<MatchingResponse> getMatching(
      @PathVariable UUID visitId,
      @CookieValue(name = CUSTOMER_TOKEN_COOKIE, required = false) String rawToken) {
    return ApiResponse.success(MatchingResponse.from(service.getMatching(visitId, rawToken)));
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

  public record OnboardingRequest(
      @NotBlank(message = "고객 이름은 비어 있을 수 없습니다.")
          @Size(max = 100, message = "고객 이름은 100자 이하여야 합니다.")
          String name,
      @NotNull(message = "서비스 이용 언어는 필수입니다.") LanguageCode serviceLanguage,
      @NotNull(message = "직원 응대 방식은 필수입니다.") InteractionStyle interactionStyle,
      @Size(max = 1000, message = "추가 요청은 1,000자 이하여야 합니다.") String additionalRequest) {

    CustomerVisitService.OnboardingCommand toCommand() {
      return new CustomerVisitService.OnboardingCommand(
          name, serviceLanguage, interactionStyle, additionalRequest);
    }
  }

  public record EntryResponse(UUID visitId, String customerName, VisitStatus status) {

    static EntryResponse from(CustomerVisitService.EntryResult result) {
      return new EntryResponse(result.visitId(), result.customerName(), result.status());
    }
  }

  public record OnboardingResponse(UUID visitId, VisitStatus status) {

    static OnboardingResponse from(CustomerVisitService.OnboardingResult result) {
      return new OnboardingResponse(result.visitId(), result.status());
    }
  }

  public record MatchingResponse(
      UUID visitId,
      VisitStatus status,
      UUID staffId,
      String staffName,
      java.time.Instant matchedAt) {

    static MatchingResponse from(CustomerVisitService.MatchingResult result) {
      return new MatchingResponse(
          result.visitId(),
          result.status(),
          result.staffId(),
          result.staffName(),
          result.matchedAt());
    }
  }
}
