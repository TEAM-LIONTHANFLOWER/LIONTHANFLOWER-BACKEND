// 직원 프로필 등록 API를 처리하는 컨트롤러
package com.lionthanflower.controller.staff;

import com.lionthanflower.application.staff.StaffProfileService;
import com.lionthanflower.application.staff.dto.StaffProfileRegisterRequest;
import com.lionthanflower.application.staff.dto.StaffRegistrationResult;
import com.lionthanflower.global.response.ApiResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
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
      @org.springframework.beans.factory.annotation.Value(
              "${app.staff-session.cookie-secure:false}")
          boolean cookieSecure,
      @org.springframework.beans.factory.annotation.Value(
              "${app.staff-session.cookie-max-age:31536000}")
          long cookieMaxAgeSeconds) {
    this.staffProfileService = staffProfileService;
    this.cookieSecure = cookieSecure;
    this.cookieMaxAgeSeconds = cookieMaxAgeSeconds;
  }

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
}
