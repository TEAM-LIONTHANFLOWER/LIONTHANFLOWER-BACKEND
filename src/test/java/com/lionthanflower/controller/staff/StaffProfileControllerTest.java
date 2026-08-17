// 직원 프로필 등록 API를 검증하는 테스트
package com.lionthanflower.controller.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionthanflower.application.staff.StaffProfileService;
import com.lionthanflower.application.staff.dto.StaffProfileResponse;
import com.lionthanflower.application.staff.dto.StaffRegistrationResult;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.store.error.StaffErrorCode;
import com.lionthanflower.global.config.CustomerApiSecurityConfig;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.error.GlobalExceptionHandler;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StaffProfileController.class)
@Import({GlobalExceptionHandler.class, CustomerApiSecurityConfig.class})
@TestPropertySource(properties = {"app.staff-session.cookie-secure=true"})
class StaffProfileControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private StaffProfileService staffProfileService;

  @Test
  void 정상_요청이면_프로필을_등록하고_토큰_쿠키를_발급한다() throws Exception {
    UUID staffId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();
    Instant createdAt = Instant.now();

    StaffProfileResponse profile =
        new StaffProfileResponse(
            staffId, storeId, "김형진", Set.of(LanguageCode.EN, LanguageCode.JA), createdAt);

    when(staffProfileService.register(any(), any()))
        .thenReturn(new StaffRegistrationResult(profile, "issued-token"));

    mockMvc
        .perform(
            post("/api/staff/me/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                    {"storeId":"%s","name":"김형진","languages":["EN","JA"]}
                                    """
                        .formatted(storeId)))
        .andExpect(status().isOk())
        .andExpect(
            header()
                .string(
                    HttpHeaders.SET_COOKIE,
                    allOf(
                        containsString("staffToken=issued-token"),
                        containsString("Path=/"),
                        containsString("Max-Age=31536000"),
                        containsString("Secure"),
                        containsString("HttpOnly"),
                        containsString("SameSite=Lax"))))
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.staffId").value(staffId.toString()))
        .andExpect(jsonPath("$.data.storeId").value(storeId.toString()))
        .andExpect(jsonPath("$.data.name").value("김형진"));
  }

  @Test
  void storeId가_없으면_공통_400을_반환한다() throws Exception {
    mockMvc
        .perform(
            post("/api/staff/me/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                    {"name":"김형진","languages":["EN"]}
                                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("COMMON-400"))
        .andExpect(jsonPath("$.error.fieldErrors[0].field").value("storeId"));
  }

  @Test
  void 이름이_100자를_초과하면_공통_400을_반환한다() throws Exception {
    mockMvc
        .perform(
            post("/api/staff/me/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                    {"storeId":"%s","name":"%s","languages":["EN"]}
                                    """
                        .formatted(UUID.randomUUID(), "가".repeat(101))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("COMMON-400"));

    verify(staffProfileService, never()).register(any(), any());
  }

  @Test
  void 언어_원소가_비어_있으면_공통_400을_반환한다() throws Exception {
    mockMvc
        .perform(
            post("/api/staff/me/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                    {"storeId":"%s","name":"김형진","languages":[" "]}
                                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("COMMON-400"));

    verify(staffProfileService, never()).register(any(), any());
  }

  @Test
  void 기존_staffToken을_서비스에_전달한다() throws Exception {
    UUID staffId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();
    StaffProfileResponse profile =
        new StaffProfileResponse(staffId, storeId, "김형진", Set.of(LanguageCode.EN), Instant.now());
    when(staffProfileService.register(any(), eq("existing-token")))
        .thenReturn(new StaffRegistrationResult(profile, "issued-token"));

    mockMvc
        .perform(
            post("/api/staff/me/profile")
                .cookie(new Cookie("staffToken", "existing-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                    {"storeId":"%s","name":"김형진","languages":["EN"]}
                                    """
                        .formatted(storeId)))
        .andExpect(status().isOk());

    verify(staffProfileService).register(any(), eq("existing-token"));
  }

  @Test
  void 이미_등록된_직원이면_409를_반환한다() throws Exception {
    UUID storeId = UUID.randomUUID();

    when(staffProfileService.register(any(), any()))
        .thenThrow(new BusinessException(StaffErrorCode.PROFILE_ALREADY_EXISTS));

    mockMvc
        .perform(
            post("/api/staff/me/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                    {"storeId":"%s","name":"김형진","languages":["EN"]}
                                    """
                        .formatted(storeId)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("STAFF-409"));
  }

  @Test
  void 인증된_직원이면_본인_프로필을_반환한다() throws Exception {
    UUID staffId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();
    Staff staff = Staff.create(storeId, "김형진", "hashed-token", Set.of(LanguageCode.EN));

    mockMvc
        .perform(
            get("/api/staff/me/profile")
                .with(
                    authentication(
                        new UsernamePasswordAuthenticationToken(staff, null, java.util.List.of()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.name").value("김형진"));
  }

  @Test
  void 인증되지_않으면_401을_반환한다() throws Exception {
    mockMvc
        .perform(get("/api/staff/me/profile"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("STAFF-401"));
  }

  @Test
  void 프로필_조회_API에_OpenAPI_설명이_있다() throws NoSuchMethodException {
    Method method = StaffProfileController.class.getMethod("getMyProfile", Staff.class);

    Operation operation = method.getAnnotation(Operation.class);

    assertThat(operation).isNotNull();
    assertThat(operation.summary()).isEqualTo("직원 프로필 조회");
  }
}
