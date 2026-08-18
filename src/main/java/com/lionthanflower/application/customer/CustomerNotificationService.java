// 고객 웹 알림의 목록 조회와 소유권 검증된 읽음 처리를 담당하는 서비스
package com.lionthanflower.application.customer;

import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.notification.entity.CustomerNotification;
import com.lionthanflower.domain.notification.entity.CustomerNotificationType;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.error.CommonErrorCode;
import com.lionthanflower.infrastructure.persistence.CustomerNotificationRepository;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerNotificationService {

  private final CustomerRepository customerRepository;
  private final CustomerNotificationRepository notificationRepository;
  private final CustomerTokenManager tokenManager;

  public CustomerNotificationService(
      CustomerRepository customerRepository,
      CustomerNotificationRepository notificationRepository,
      CustomerTokenManager tokenManager) {
    this.customerRepository = customerRepository;
    this.notificationRepository = notificationRepository;
    this.tokenManager = tokenManager;
  }

  public List<NotificationView> getNotifications(String rawToken) {
    Customer customer = requireCustomer(rawToken);
    return notificationRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId()).stream()
        .map(this::toView)
        .toList();
  }

  @Transactional
  public NotificationView markRead(UUID notificationId, String rawToken) {
    Customer customer = requireCustomer(rawToken);
    CustomerNotification notification =
        notificationRepository
            .findByIdAndCustomerId(notificationId, customer.getId())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
    notification.markRead(Instant.now());
    notificationRepository.save(notification);
    return toView(notification);
  }

  private Customer requireCustomer(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
    }
    return customerRepository
        .findByTokenHash(tokenManager.hash(rawToken))
        .orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));
  }

  private NotificationView toView(CustomerNotification notification) {
    return new NotificationView(
        notification.getId(),
        notification.getType(),
        notification.getResourceId(),
        notification.getMessage(),
        notification.isRead(),
        notification.getReadAt(),
        notification.getCreatedAt());
  }

  public record NotificationView(
      UUID notificationId,
      CustomerNotificationType type,
      UUID resourceId,
      String message,
      boolean read,
      Instant readAt,
      Instant createdAt) {}
}
