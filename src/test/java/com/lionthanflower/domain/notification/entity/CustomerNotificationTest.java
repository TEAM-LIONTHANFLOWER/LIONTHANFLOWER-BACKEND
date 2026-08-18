// 고객 웹 알림 생성과 멱등 읽음 상태를 검증하는 테스트
package com.lionthanflower.domain.notification.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerNotificationTest {

  @Test
  void Visit_Memory_알림을_생성하고_읽음_처리한다() {
    UUID customerId = UUID.randomUUID();
    UUID visitMemoryId = UUID.randomUUID();
    Instant readAt = Instant.parse("2026-08-18T06:00:00Z");

    CustomerNotification notification =
        CustomerNotification.createVisitMemory(
            customerId, visitMemoryId, "새로운 Visit Memory가 도착했습니다.");

    assertThat(notification.getCustomerId()).isEqualTo(customerId);
    assertThat(notification.getResourceId()).isEqualTo(visitMemoryId);
    assertThat(notification.getType()).isEqualTo(CustomerNotificationType.VISIT_MEMORY);
    assertThat(notification.isRead()).isFalse();

    notification.markRead(readAt);
    notification.markRead(readAt.plusSeconds(10));

    assertThat(notification.isRead()).isTrue();
    assertThat(notification.getReadAt()).isEqualTo(readAt);
  }
}
