// 방문 엔티티의 데이터베이스 조회와 저장을 담당하는 Repository
package com.lionthanflower.infrastructure.persistence;

import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitRepository extends JpaRepository<Visit, UUID> {
  List<Visit> findByStoreIdAndStatusIn(UUID storeId, Collection<VisitStatus> statuses);

  List<Visit> findByStoreIdAndStaffIdAndStatus(UUID storeId, UUID staffId, VisitStatus status);

  Optional<Visit> findByIdAndStoreId(UUID id, UUID storeId);
}
