// 고객 웹 알림 목록과 읽음 처리를 검증하는 테스트
package com.lionthanflower.application.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.notification.entity.CustomerNotification;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.error.CommonErrorCode;
import com.lionthanflower.infrastructure.persistence.CustomerNotificationRepository;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerNotificationServiceTest {

  @Mock private CustomerRepository customerRepository;
  @Mock private CustomerNotificationRepository notificationRepository;
  @Mock private CustomerTokenManager tokenManager;

  private CustomerNotificationService service;

  @BeforeEach
  void setUp() {
    service =
        new CustomerNotificationService(customerRepository, notificationRepository, tokenManager);
  }

  @Test
  void 고객_알림을_최신순_목록으로_반환한다() {
    Customer customer = customer();
    CustomerNotification notification =
        CustomerNotification.createVisitMemory(customer.getId(), UUID.randomUUID(), "새로운 기록");
    when(tokenManager.hash("raw-token")).thenReturn("hashed-token");
    when(customerRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(customer));
    when(notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()))
        .thenReturn(List.of(notification));

    var result = service.getNotifications("raw-token");

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().message()).isEqualTo("새로운 기록");
    assertThat(result.getFirst().read()).isFalse();
  }

  @Test
  void 고객_알림을_멱등하게_읽음_처리한다() {
    Customer customer = customer();
    CustomerNotification notification =
        CustomerNotification.createVisitMemory(customer.getId(), UUID.randomUUID(), "새로운 기록");
    when(tokenManager.hash("raw-token")).thenReturn("hashed-token");
    when(customerRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(customer));
    when(notificationRepository.findByIdAndCustomerId(notification.getId(), customer.getId()))
        .thenReturn(Optional.of(notification));

    var result = service.markRead(notification.getId(), "raw-token");
    var firstReadAt = result.readAt();
    var secondResult = service.markRead(notification.getId(), "raw-token");

    assertThat(result.read()).isTrue();
    assertThat(secondResult.readAt()).isEqualTo(firstReadAt);
    verify(notificationRepository, times(2)).save(notification);
  }

  @Test
  void 다른_고객의_알림은_읽음_처리할_수_없다() {
    Customer customer = customer();
    UUID notificationId = UUID.randomUUID();
    when(tokenManager.hash("raw-token")).thenReturn("hashed-token");
    when(customerRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(customer));
    when(notificationRepository.findByIdAndCustomerId(notificationId, customer.getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.markRead(notificationId, "raw-token"))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(CommonErrorCode.NOT_FOUND));
  }

  @Test
  void 고객_토큰이_없거나_유효하지_않으면_알림_읽음_처리는_실패한다() {
    UUID notificationId = UUID.randomUUID();

    assertThatThrownBy(() -> service.markRead(notificationId, null))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED));

    when(tokenManager.hash("invalid-token")).thenReturn("invalid-hash");
    when(customerRepository.findByTokenHash("invalid-hash")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.markRead(notificationId, "invalid-token"))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(CommonErrorCode.UNAUTHORIZED));
  }

  private Customer customer() {
    return Customer.create("hashed-token");
  }
}
