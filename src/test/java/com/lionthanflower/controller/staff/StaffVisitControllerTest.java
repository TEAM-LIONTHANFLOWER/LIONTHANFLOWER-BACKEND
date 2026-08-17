// 직원의 현재 방문 고객 목록 조회 API를 검증하는 테스트
package com.lionthanflower.controller.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionthanflower.application.staff.StaffVisitService;
import com.lionthanflower.application.staff.dto.VisitSummaryResponse;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.visit.entity.InteractionStyle;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import com.lionthanflower.global.config.CustomerApiSecurityConfig;
import com.lionthanflower.global.error.GlobalExceptionHandler;
import io.swagger.v3.oas.annotations.Operation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(StaffVisitController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, CustomerApiSecurityConfig.class})
class StaffVisitControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private StaffVisitService staffVisitService;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void 인증된_직원의_현재_방문_고객_목록을_반환한다() throws Exception {
    UUID storeId = UUID.randomUUID();
    Staff staff = Staff.create(storeId, "김형진", "hashed-token", Set.of(LanguageCode.EN));
    UUID visitId = UUID.randomUUID();

    when(staffVisitService.getCurrentVisits(any(UUID.class)))
        .thenReturn(
            List.of(
                new VisitSummaryResponse(
                    visitId,
                    "홍길동",
                    VisitStatus.WAITING_FOR_STAFF,
                    LanguageCode.EN,
                    InteractionStyle.STAFF_RECOMMENDATION,
                    staff.getId())));

    MvcResult result =
        mockMvc
            .perform(
                get("/api/staff/visits")
                    .with(
                        authentication(
                            new UsernamePasswordAuthenticationToken(staff, null, List.of()))))
            .andReturn();

    verify(staffVisitService).getCurrentVisits(storeId);
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentAsString())
        .contains(visitId.toString(), "홍길동", "WAITING_FOR_STAFF");
  }

  @Test
  void 인증되지_않으면_401을_반환한다() throws Exception {
    mockMvc
        .perform(get("/api/staff/visits").with(anonymous()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("STAFF-401"));

    verify(staffVisitService, never()).getCurrentVisits(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void 현재_방문_고객이_없으면_빈_목록을_반환한다() throws Exception {
    UUID storeId = UUID.randomUUID();
    Staff staff = Staff.create(storeId, "김형진", "hashed-token", Set.of(LanguageCode.EN));
    when(staffVisitService.getCurrentVisits(storeId)).thenReturn(List.of());

    mockMvc
        .perform(
            get("/api/staff/visits")
                .with(
                    authentication(
                        new UsernamePasswordAuthenticationToken(staff, null, List.of()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.visits.length()").value(0));
  }

  @Test
  void 현재_방문_고객_조회_API에_OpenAPI_설명이_있다() throws NoSuchMethodException {
    Method method = StaffVisitController.class.getMethod("getCurrentVisits", Staff.class);

    Operation operation = method.getAnnotation(Operation.class);

    assertThat(operation).isNotNull();
    assertThat(operation.summary()).isEqualTo("현재 방문 고객 목록 조회");
  }
}
