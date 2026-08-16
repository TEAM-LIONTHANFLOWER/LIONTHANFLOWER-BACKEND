// 고객 서비스 진입 HTTP API를 검증하는 테스트
package com.lionthanflower.infrastructure.web.customer;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerVisitController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
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
}
