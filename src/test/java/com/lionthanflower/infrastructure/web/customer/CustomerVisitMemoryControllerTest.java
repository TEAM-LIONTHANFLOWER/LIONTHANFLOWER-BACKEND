// 고객 Visit Memory 상세 조회 HTTP API를 검증하는 테스트
package com.lionthanflower.infrastructure.web.customer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionthanflower.application.customer.CustomerVisitMemoryQueryService;
import com.lionthanflower.global.config.CustomerApiSecurityConfig;
import com.lionthanflower.global.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerVisitMemoryController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, CustomerApiSecurityConfig.class})
class CustomerVisitMemoryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CustomerVisitMemoryQueryService service;

  @Test
  void 고객_Visit_Memory_상세를_조회한다() throws Exception {
    UUID memoryId = UUID.randomUUID();
    when(service.getMemory(memoryId, "raw-token"))
        .thenReturn(
            new CustomerVisitMemoryQueryService.VisitMemoryDetail(
                memoryId,
                UUID.randomUUID(),
                "MCM HAUS",
                "KR",
                "다음 방문을 준비한 기록",
                Instant.parse("2026-08-18T06:00:00Z")));

    mockMvc
        .perform(
            get("/api/customers/visit-memories/{visitMemoryId}", memoryId)
                .cookie(new jakarta.servlet.http.Cookie("customer_token", "raw-token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.visitMemoryId").value(memoryId.toString()))
        .andExpect(jsonPath("$.data.summary").value("다음 방문을 준비한 기록"));

    verify(service).getMemory(memoryId, "raw-token");
  }
}
