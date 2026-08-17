// 고객별 공개 Arc의 목록과 상세 소유권 조회를 담당하는 Repository
package com.lionthanflower.infrastructure.persistence;

import com.lionthanflower.domain.arc.entity.Arc;
import com.lionthanflower.domain.arc.entity.ArcStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArcRepository extends JpaRepository<Arc, UUID> {

  List<Arc> findByCustomerIdAndStatusInOrderByArcNumberDesc(
      UUID customerId, Collection<ArcStatus> statuses);

  List<Arc> findByCustomerIdInAndStatusIn(
      Collection<UUID> customerIds, Collection<ArcStatus> statuses);

  Optional<Arc> findByVisitId(UUID visitId);

  long countByCustomerIdAndStatusIn(UUID customerId, Collection<ArcStatus> statuses);

  Optional<Arc> findByIdAndCustomerIdAndStatusIn(
      UUID id, UUID customerId, Collection<ArcStatus> statuses);
}
