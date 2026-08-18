// 고객 웹 알림의 최신순 조회와 소유권 조회를 담당하는 Repository
package com.lionthanflower.infrastructure.persistence;

import com.lionthanflower.domain.notification.entity.CustomerNotification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerNotificationRepository extends JpaRepository<CustomerNotification, UUID> {

  List<CustomerNotification> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

  Optional<CustomerNotification> findByIdAndCustomerId(UUID id, UUID customerId);

  boolean existsByCustomerIdAndResourceId(UUID customerId, UUID resourceId);
}
