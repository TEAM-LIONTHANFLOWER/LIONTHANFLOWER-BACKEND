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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionthanflower.application.staff.StaffVisitService;
import com.lionthanflower.application.staff.dto.StaffVisitAssignmentResponse;
import com.lionthanflower.application.staff.dto.VisitResultType;
import com.lionthanflower.application.staff.dto.VisitSummaryResponse;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.visit.entity.InteractionStyle;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import com.lionthanflower.domain.visit.error.VisitErrorCode;
import com.lionthanflower.global.config.CustomerApiSecurityConfig;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.error.GlobalExceptionHandler;
import io.swagger.v3.oas.annotations.Operation;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
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
    Instant visitedAt = Instant.parse("2026-08-19T00:20:00Z");
    Instant matchedAt = Instant.parse("2026-08-19T00:24:00Z");
    Visit visit = Visit.create(UUID.randomUUID(), storeId);
    visit.completeOnboarding(
        LanguageCode.EN, InteractionStyle.STAFF_RECOMMENDATION, "다양한 컬러를 보고 싶어요");
    visit.assignStaff(staff.getId(), matchedAt);
    ReflectionTestUtils.setField(visit, "id", visitId);
    ReflectionTestUtils.setField(visit, "createdAt", visitedAt);
    UUID resultId = UUID.randomUUID();

    when(staffVisitService.getCurrentVisits(any(UUID.class), any(UUID.class)))
        .thenReturn(
            List.of(
                VisitSummaryResponse.of(
                    visit, "홍길동", 2, resultId, null, VisitResultType.ARC, resultId, 2)));

    MvcResult result =
        mockMvc
            .perform(
                get("/api/staff/visits")
                    .with(
                        authentication(
                            new UsernamePasswordAuthenticationToken(staff, null, List.of()))))
            .andReturn();

    verify(staffVisitService).getCurrentVisits(storeId, staff.getId());
    assertThat(result.getResponse().getStatus()).isEqualTo(200);
    assertThat(result.getResponse().getContentAsString())
        .contains(
            visitId.toString(),
            "홍길동",
            "ACTIVE",
            "다양한 컬러를 보고 싶어요",
            "\"arcCount\":2",
            "\"resultType\":\"ARC\"",
            "\"resultId\":\"" + resultId + "\"",
            "\"arcNumber\":2",
            "\"matchedAt\":\"2026-08-19T00:24:00Z\"",
            "\"visitedAt\":\"2026-08-19T00:20:00Z\"");
  }

  @Test
  void 인증되지_않으면_401을_반환한다() throws Exception {
    mockMvc
        .perform(get("/api/staff/visits").with(anonymous()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("STAFF-401"));

    verify(staffVisitService, never())
        .getCurrentVisits(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void 현재_방문_고객이_없으면_빈_목록을_반환한다() throws Exception {
    UUID storeId = UUID.randomUUID();
    Staff staff = Staff.create(storeId, "김형진", "hashed-token", Set.of(LanguageCode.EN));
    when(staffVisitService.getCurrentVisits(storeId, staff.getId())).thenReturn(List.of());

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
    assertThat(operation.summary()).isEqualTo("직원 방문 고객 목록 조회");
    assertThat(operation.description()).contains("진행·대기 방문", "정상 종료 방문", "결과 정보");
  }

  @Test
  void 응대_시작_요청으로_현재_직원을_담당자로_배정한다() throws Exception {
    UUID visitId = UUID.randomUUID();
    Staff staff = Staff.create(UUID.randomUUID(), "김형진", "hashed-token", Set.of(LanguageCode.EN));
    Instant matchedAt = Instant.parse("2026-08-18T03:00:00Z");
    when(staffVisitService.assignVisit(visitId, staff))
        .thenReturn(
            new StaffVisitAssignmentResponse(
                visitId, staff.getId(), VisitStatus.ACTIVE, matchedAt));

    mockMvc
        .perform(
            post("/api/staff/visits/{visitId}/assignment", visitId)
                .with(
                    authentication(
                        new UsernamePasswordAuthenticationToken(staff, null, List.of()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.visitId").value(visitId.toString()))
        .andExpect(jsonPath("$.data.staffId").value(staff.getId().toString()))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"))
        .andExpect(jsonPath("$.data.matchedAt").value(matchedAt.toString()));

    verify(staffVisitService).assignVisit(visitId, staff);
  }

  @Test
  void 미인증_응대_시작_요청은_401을_반환한다() throws Exception {
    mockMvc
        .perform(
            post("/api/staff/visits/{visitId}/assignment", UUID.randomUUID()).with(anonymous()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("STAFF-401"));

    verify(staffVisitService, never())
        .assignVisit(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void 동시_매칭_충돌은_409를_반환한다() throws Exception {
    UUID visitId = UUID.randomUUID();
    Staff staff = Staff.create(UUID.randomUUID(), "김형진", "hashed-token", Set.of(LanguageCode.EN));
    when(staffVisitService.assignVisit(visitId, staff))
        .thenThrow(new ObjectOptimisticLockingFailureException(Visit.class, visitId));

    mockMvc
        .perform(
            post("/api/staff/visits/{visitId}/assignment", visitId)
                .with(
                    authentication(
                        new UsernamePasswordAuthenticationToken(staff, null, List.of()))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("COMMON-409"));
  }

  @Test
  void 방문을_찾지_못하면_404와_VISIT_404를_반환한다() throws Exception {
    UUID visitId = UUID.randomUUID();
    Staff staff = Staff.create(UUID.randomUUID(), "김형진", "hashed-token", Set.of(LanguageCode.EN));
    when(staffVisitService.assignVisit(visitId, staff))
        .thenThrow(new BusinessException(VisitErrorCode.NOT_FOUND));

    mockMvc
        .perform(
            post("/api/staff/visits/{visitId}/assignment", visitId)
                .with(
                    authentication(
                        new UsernamePasswordAuthenticationToken(staff, null, List.of()))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("VISIT-404"));
  }

  @Test
  void 매칭할_수_없는_방문은_409와_VISIT_409를_반환한다() throws Exception {
    UUID visitId = UUID.randomUUID();
    Staff staff = Staff.create(UUID.randomUUID(), "김형진", "hashed-token", Set.of(LanguageCode.EN));
    when(staffVisitService.assignVisit(visitId, staff))
        .thenThrow(new BusinessException(VisitErrorCode.NOT_ASSIGNABLE));

    mockMvc
        .perform(
            post("/api/staff/visits/{visitId}/assignment", visitId)
                .with(
                    authentication(
                        new UsernamePasswordAuthenticationToken(staff, null, List.of()))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("VISIT-409"));
  }

  @Test
  void 응대_시작_API에_OpenAPI_설명이_있다() throws NoSuchMethodException {
    Method method = StaffVisitController.class.getMethod("assignVisit", UUID.class, Staff.class);

    Operation operation = method.getAnnotation(Operation.class);

    assertThat(operation).isNotNull();
    assertThat(operation.summary()).isEqualTo("직원 추천 고객 응대 시작");
  }
}
