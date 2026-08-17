// 직원 프로필 등록 API의 보안 설정(permitAll, CSRF 예외)을 검증하는 테스트
package com.lionthanflower.controller.staff;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionthanflower.application.staff.StaffProfileService;
import com.lionthanflower.application.staff.dto.StaffProfileResponse;
import com.lionthanflower.application.staff.dto.StaffRegistrationResult;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.global.config.CustomerApiSecurityConfig;
import com.lionthanflower.global.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StaffProfileController.class)
@Import({GlobalExceptionHandler.class, CustomerApiSecurityConfig.class})
class StaffProfileControllerSecurityTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private StaffProfileService staffProfileService;

  @Test
  void 인증_쿠키와_CSRF_토큰_없이도_요청이_필터를_통과한다() throws Exception {
    UUID staffId = UUID.randomUUID();
    UUID storeId = UUID.randomUUID();

    StaffProfileResponse profile =
        new StaffProfileResponse(staffId, storeId, "김형진", Set.of(LanguageCode.EN), Instant.now());

    when(staffProfileService.register(any(), any()))
        .thenReturn(new StaffRegistrationResult(profile, "issued-token"));

    mockMvc
        .perform(
            post("/api/staff/me/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                        {"storeId":"%s","name":"김형진","languages":["EN"]}
                                        """
                        .formatted(storeId)))
        .andExpect(status().isOk());
  }
}
