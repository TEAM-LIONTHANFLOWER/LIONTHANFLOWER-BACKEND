// 직원 프로필 등록/조회 API를 처리하는 컨트롤러
package com.lionthanflower.controller.staff;

import com.lionthanflower.application.staff.StaffProfileService;
import com.lionthanflower.application.staff.dto.StaffProfileRegisterRequest;
import com.lionthanflower.application.staff.dto.StaffProfileResponse;
import com.lionthanflower.application.staff.dto.StaffRegistrationResult;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.store.error.StaffErrorCode;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaffProfileController {
  private static final String STAFF_TOKEN_COOKIE_NAME = "staffToken";

  private final StaffProfileService staffProfileService;
  private final boolean cookieSecure;
  private final long cookieMaxAgeSeconds;

  public StaffProfileController(
      StaffProfileService staffProfileService,
      @Value("${app.staff-session.cookie-secure:false}") boolean cookieSecure,
      @Value("${app.staff-session.cookie-max-age:31536000}") long cookieMaxAgeSeconds) {
    this.staffProfileService = staffProfileService;
    this.cookieSecure = cookieSecure;
    this.cookieMaxAgeSeconds = cookieMaxAgeSeconds;
  }

  @Operation(summary = "직원 프로필 등록", description = "근무 매장과 구사 언어를 등록하고 직원 인증 쿠키를 발급합니다.")
  @PostMapping("/api/staff/me/profile")
  public ResponseEntity<ApiResponse<?>> register(
      @Valid @RequestBody StaffProfileRegisterRequest request,
      @CookieValue(value = STAFF_TOKEN_COOKIE_NAME, required = false) String existingToken) {

    StaffRegistrationResult result = staffProfileService.register(request, existingToken);

    ResponseCookie cookie =
        ResponseCookie.from(STAFF_TOKEN_COOKIE_NAME, result.rawToken())
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .sameSite("Lax")
            .maxAge(Duration.ofSeconds(cookieMaxAgeSeconds))
            .build();

    return ResponseEntity.status(HttpStatus.OK)
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(ApiResponse.success(result.profile()));
  }

  @Operation(summary = "직원 프로필 조회", description = "staffToken 쿠키로 인증된 직원의 프로필을 조회합니다.")
  @GetMapping("/api/staff/me/profile")
  public ApiResponse<StaffProfileResponse> getMyProfile(@AuthenticationPrincipal Staff staff) {
    if (staff == null) {
      throw new BusinessException(StaffErrorCode.UNAUTHORIZED);
    }
    return ApiResponse.success(StaffProfileResponse.from(staff));
  }
}
