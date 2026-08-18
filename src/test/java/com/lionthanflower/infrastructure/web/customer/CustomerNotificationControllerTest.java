// 고객 알림 목록과 읽음 처리 HTTP API를 검증하는 테스트
package com.lionthanflower.infrastructure.web.customer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionthanflower.application.customer.CustomerNotificationService;
import com.lionthanflower.domain.notification.entity.CustomerNotificationType;
import com.lionthanflower.global.config.CustomerApiSecurityConfig;
import com.lionthanflower.global.error.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerNotificationController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, CustomerApiSecurityConfig.class})
class CustomerNotificationControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CustomerNotificationService service;

  @Test
  void 고객_알림을_최신순으로_조회한다() throws Exception {
    UUID resourceId = UUID.randomUUID();
    when(service.getNotifications("raw-token"))
        .thenReturn(
            List.of(
                new CustomerNotificationService.NotificationView(
                    UUID.randomUUID(),
                    CustomerNotificationType.VISIT_MEMORY,
                    resourceId,
                    "새로운 기록",
                    false,
                    null,
                    Instant.parse("2026-08-18T06:00:00Z"))));

    mockMvc
        .perform(
            get("/api/customers/notifications")
                .cookie(new jakarta.servlet.http.Cookie("customer_token", "raw-token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].resourceId").value(resourceId.toString()))
        .andExpect(jsonPath("$.data[0].read").value(false));

    verify(service).getNotifications("raw-token");
  }

  @Test
  void 고객_알림을_읽음_처리한다() throws Exception {
    UUID notificationId = UUID.randomUUID();
    when(service.markRead(notificationId, "raw-token"))
        .thenReturn(
            new CustomerNotificationService.NotificationView(
                notificationId,
                CustomerNotificationType.VISIT_MEMORY,
                UUID.randomUUID(),
                "새로운 기록",
                true,
                Instant.parse("2026-08-18T06:00:00Z"),
                Instant.parse("2026-08-18T05:00:00Z")));

    mockMvc
        .perform(
            patch("/api/customers/notifications/{notificationId}/read", notificationId)
                .cookie(new jakarta.servlet.http.Cookie("customer_token", "raw-token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.read").value(true));

    verify(service).markRead(notificationId, "raw-token");
  }
}
