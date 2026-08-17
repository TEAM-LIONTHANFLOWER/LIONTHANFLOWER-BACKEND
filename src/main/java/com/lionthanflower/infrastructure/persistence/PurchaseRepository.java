// 방문별 구매 집계 조회와 저장을 담당하는 Repository
package com.lionthanflower.infrastructure.persistence;

import com.lionthanflower.domain.purchase.entity.Purchase;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {
  Optional<Purchase> findByVisitId(UUID visitId);
}
