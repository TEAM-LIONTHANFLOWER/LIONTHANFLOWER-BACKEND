// 고객 서비스 진입과 온보딩 진행 HTTP API를 검증하는 테스트
package com.lionthanflower.infrastructure.web.customer;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionthanflower.application.customer.CustomerVisitService;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import com.lionthanflower.global.error.GlobalExceptionHandler;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerVisitController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@TestPropertySource(properties = "app.customer-api-security.enabled=false")
class CustomerVisitControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CustomerVisitService service;

  @Test
  void 신규_고객의_서비스_진입은_방문과_쿠키를_반환한다() throws Exception {
    UUID visitId = UUID.randomUUID();
    when(service.enter(null))
        .thenReturn(
            new CustomerVisitService.EntryResult(
                visitId, null, VisitStatus.ONBOARDING, "issued-token"));

    mockMvc
        .perform(post("/api/customers/visits"))
        .andExpect(status().isCreated())
        .andExpect(
            header().string(HttpHeaders.SET_COOKIE, containsString("customer_token=issued-token")))
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.visitId").value(visitId.toString()))
        .andExpect(jsonPath("$.data.customerName").isEmpty())
        .andExpect(jsonPath("$.data.status").value("ONBOARDING"));
  }

  @Test
  void 기존_고객의_서비스_진입은_새_쿠키를_발급하지_않는다() throws Exception {
    UUID visitId = UUID.randomUUID();
    when(service.enter("known-token"))
        .thenReturn(
            new CustomerVisitService.EntryResult(visitId, "홍길동", VisitStatus.ONBOARDING, null));

    mockMvc
        .perform(
            post("/api/customers/visits")
                .cookie(new jakarta.servlet.http.Cookie("customer_token", "known-token")))
        .andExpect(status().isCreated())
        .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
        .andExpect(jsonPath("$.data.customerName").value("홍길동"));
  }

  @Test
  void 고객_온보딩_진행은_방문_상태를_반환한다() throws Exception {
    UUID visitId = UUID.randomUUID();
    when(service.progressOnboarding(any(), any(), any()))
        .thenReturn(
            new CustomerVisitService.OnboardingResult(visitId, VisitStatus.WAITING_FOR_STAFF));

    mockMvc
        .perform(
            patch("/api/customers/visits/{visitId}/onboarding", visitId)
                .cookie(new jakarta.servlet.http.Cookie("customer_token", "known-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":"홍길동","serviceLanguage":"EN","interactionStyle":"STAFF_RECOMMENDATION","additionalRequest":"가방 추천"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.visitId").value(visitId.toString()))
        .andExpect(jsonPath("$.data.status").value("WAITING_FOR_STAFF"));
  }

  @Test
  void 고객_온보딩_진행은_이름_검증에_실패하면_공통_400을_반환한다() throws Exception {
    UUID visitId = UUID.randomUUID();

    mockMvc
        .perform(
            patch("/api/customers/visits/{visitId}/onboarding", visitId)
                .cookie(new jakarta.servlet.http.Cookie("customer_token", "known-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"name":" ","serviceLanguage":"EN","interactionStyle":"SELF_GUIDED"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error.code").value("COMMON-400"))
        .andExpect(jsonPath("$.error.fieldErrors[0].field").value("name"));

    verify(service, never()).progressOnboarding(any(), any(), any());
  }

  @Test
  void 고객_온보딩_진행은_JSON이_아니면_공통_415를_반환한다() throws Exception {
    mockMvc
        .perform(
            patch("/api/customers/visits/{visitId}/onboarding", UUID.randomUUID())
                .contentType(MediaType.TEXT_PLAIN)
                .content("name"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.error.code").value("COMMON-415"));
  }
}
