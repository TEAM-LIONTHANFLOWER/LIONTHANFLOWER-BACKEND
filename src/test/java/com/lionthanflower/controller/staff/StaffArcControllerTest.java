// 직원 Arc 생성·조회·재생성·공유 API를 검증하는 테스트
package com.lionthanflower.controller.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionthanflower.application.staff.StaffArcService;
import com.lionthanflower.application.staff.dto.StaffArcGenerationRequest;
import com.lionthanflower.application.staff.dto.StaffArcRevisionResponse;
import com.lionthanflower.domain.arc.entity.ArcGeneratedContent;
import com.lionthanflower.domain.arc.entity.ArcRevisionStatus;
import com.lionthanflower.domain.arc.entity.ArcStatus;
import com.lionthanflower.domain.arc.error.ArcErrorCode;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.global.config.CustomerApiSecurityConfig;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.error.GlobalExceptionHandler;
import io.swagger.v3.oas.annotations.Operation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StaffArcController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, CustomerApiSecurityConfig.class})
class StaffArcControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private StaffArcService staffArcService;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void Arc_생성_요청은_생성_중인_리비전을_반환한다() throws Exception {
    UUID visitId = UUID.randomUUID();
    Staff staff = staff();
    StaffArcRevisionResponse response = response(visitId, ArcRevisionStatus.GENERATING);
    when(staffArcService.createArc(any(), any(), any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/staff/visits/{visitId}/arcs", visitId)
                .contentType("application/json")
                .content("{}")
                .with(authentication(staffAuthentication(staff))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.arcId").value(response.arcId().toString()))
        .andExpect(jsonPath("$.data.revisionStatus").value("GENERATING"));

    verify(staffArcService).createArc(any(), any(), any());
  }

  @Test
  void Arc_생성_요청은_구매_날짜와_국가와_매장을_입력받는다() throws Exception {
    UUID visitId = UUID.randomUUID();
    Staff staff = staff();
    StaffArcRevisionResponse response = response(visitId, ArcRevisionStatus.GENERATING);
    when(staffArcService.createArc(any(), any(), any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/staff/visits/{visitId}/arcs", visitId)
                .contentType("application/json")
                .content(
                    """
                    {
                      "inputSnapshot": {
                        "purchaseDate": "2026-08-13",
                        "purchaseCountry": "KOREA",
                        "purchaseStore": "MCM HAUS",
                        "purchasedProductVariantIds": ["%s"],
                        "preferredCategories": ["BAG"],
                        "preferredColors": ["BLACK"],
                        "preferredStyles": ["MINIMAL_SIMPLE"],
                        "interestedProductVariantIds": [],
                        "purchaseCriteria": ["DESIGN"],
                        "interactionPreferences": ["ACTIVE_RECOMMENDATION"],
                        "explanationPreferences": ["KEY_POINTS_ONLY"],
                        "purchaseDecisionStyle": "QUICK",
                        "staffObservation": "차분한 응대를 선호함"
                      }
                    }
                    """
                        .formatted(UUID.randomUUID()))
                .with(authentication(staffAuthentication(staff))))
        .andExpect(status().isOk());

    ArgumentCaptor<StaffArcGenerationRequest> requestCaptor =
        ArgumentCaptor.forClass(StaffArcGenerationRequest.class);
    verify(staffArcService).createArc(eq(visitId), eq(staff), requestCaptor.capture());
    assertThat(requestCaptor.getValue().inputSnapshot().purchaseDate().toString())
        .isEqualTo("2026-08-13");
    assertThat(requestCaptor.getValue().inputSnapshot().purchaseCountry()).isEqualTo("KOREA");
    assertThat(requestCaptor.getValue().inputSnapshot().purchaseStore()).isEqualTo("MCM HAUS");
  }

  @Test
  void Arc_미리보기_조회는_현재_리비전과_생성_결과를_반환한다() throws Exception {
    UUID arcId = UUID.randomUUID();
    Staff staff = staff();
    StaffArcRevisionResponse response = response(arcId, ArcRevisionStatus.READY);
    when(staffArcService.getPreview(arcId, staff)).thenReturn(response);

    mockMvc
        .perform(
            get("/api/staff/arcs/{arcId}", arcId).with(authentication(staffAuthentication(staff))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.arcId").value(arcId.toString()))
        .andExpect(jsonPath("$.data.revisionStatus").value("READY"))
        .andExpect(jsonPath("$.data.generatedContent.momentSummary").value("오늘의 순간"));
  }

  @Test
  void Arc_재생성_요청은_새_리비전을_생성한다() throws Exception {
    UUID arcId = UUID.randomUUID();
    Staff staff = staff();
    StaffArcRevisionResponse response = response(arcId, ArcRevisionStatus.GENERATING);
    when(staffArcService.regenerate(any(), any(), any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/staff/arcs/{arcId}/revisions", arcId)
                .contentType("application/json")
                .content("{}")
                .with(authentication(staffAuthentication(staff))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.revisionStatus").value("GENERATING"));

    verify(staffArcService).regenerate(any(), any(), any());
  }

  @Test
  void READY_리비전을_고객에게_공유한다() throws Exception {
    UUID arcId = UUID.randomUUID();
    UUID revisionId = UUID.randomUUID();
    Staff staff = staff();
    StaffArcRevisionResponse response = response(arcId, ArcRevisionStatus.READY);
    when(staffArcService.share(arcId, revisionId, staff)).thenReturn(response);

    mockMvc
        .perform(
            post("/api/staff/arcs/{arcId}/revisions/{revisionId}/share", arcId, revisionId)
                .with(authentication(staffAuthentication(staff))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.arcStatus").value("SHARED"));

    verify(staffArcService).share(arcId, revisionId, staff);
  }

  @Test
  void Arc를_찾지_못하면_404와_ARC_404를_반환한다() throws Exception {
    UUID arcId = UUID.randomUUID();
    Staff staff = staff();
    when(staffArcService.getPreview(arcId, staff))
        .thenThrow(new BusinessException(ArcErrorCode.NOT_FOUND));

    mockMvc
        .perform(
            get("/api/staff/arcs/{arcId}", arcId).with(authentication(staffAuthentication(staff))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error.code").value("ARC-404"));
  }

  @Test
  void 공유할_수_없는_Arc는_409와_ARC_409를_반환한다() throws Exception {
    UUID arcId = UUID.randomUUID();
    UUID revisionId = UUID.randomUUID();
    Staff staff = staff();
    when(staffArcService.share(arcId, revisionId, staff))
        .thenThrow(new BusinessException(ArcErrorCode.REVISION_NOT_READY));

    mockMvc
        .perform(
            post("/api/staff/arcs/{arcId}/revisions/{revisionId}/share", arcId, revisionId)
                .with(authentication(staffAuthentication(staff))))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error.code").value("ARC-409"));
  }

  @Test
  void 미인증_Arc_생성_요청은_401을_반환한다() throws Exception {
    mockMvc
        .perform(
            post("/api/staff/visits/{visitId}/arcs", UUID.randomUUID())
                .contentType("application/json")
                .content("{}")
                .with(anonymous()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("STAFF-401"));

    verify(staffArcService, never()).createArc(any(), any(), any());
  }

  @Test
  void Arc_생성_API에_OpenAPI_설명이_있다() throws NoSuchMethodException {
    Method method =
        StaffArcController.class.getMethod(
            "createArc", UUID.class, StaffArcGenerationRequest.class, Staff.class);

    Operation operation = method.getAnnotation(Operation.class);

    assertThat(operation).isNotNull();
    assertThat(operation.summary()).isEqualTo("Arc 생성");
  }

  private Staff staff() {
    return Staff.create(UUID.randomUUID(), "김형진", "hashed-token", Set.of(LanguageCode.EN));
  }

  private UsernamePasswordAuthenticationToken staffAuthentication(Staff staff) {
    return new UsernamePasswordAuthenticationToken(staff, null, List.of());
  }

  private StaffArcRevisionResponse response(UUID id, ArcRevisionStatus revisionStatus) {
    return new StaffArcRevisionResponse(
        id,
        UUID.randomUUID(),
        1,
        revisionStatus == ArcRevisionStatus.READY ? ArcStatus.SHARED : ArcStatus.DRAFT,
        revisionStatus,
        null,
        revisionStatus == ArcRevisionStatus.READY
            ? new ArcGeneratedContent("오늘의 순간", List.of("실용성"), "기억할 순간")
            : null,
        null,
        null);
  }
}
