// 고객 엔티티의 데이터베이스 조회와 저장을 담당하는 Repository
package com.lionthanflower.infrastructure.persistence;

import com.lionthanflower.domain.customer.entity.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

  Optional<Customer> findByTokenHash(String tokenHash);
}
