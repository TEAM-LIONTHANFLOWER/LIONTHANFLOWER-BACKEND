// 구매에 포함된 제품 Variant 항목의 조회와 저장을 담당하는 Repository
package com.lionthanflower.infrastructure.persistence;

import com.lionthanflower.domain.purchase.entity.PurchaseItem;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, UUID> {
  void deleteByPurchaseId(UUID purchaseId);
}
