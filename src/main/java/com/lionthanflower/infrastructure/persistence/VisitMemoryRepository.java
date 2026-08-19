// Visit Memory의 방문별 유일 조회와 직원·고객 소유권 조회를 담당하는 Repository
package com.lionthanflower.infrastructure.persistence;

import com.lionthanflower.domain.visitmemory.entity.VisitMemory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitMemoryRepository extends JpaRepository<VisitMemory, UUID> {

  Optional<VisitMemory> findByVisitId(UUID visitId);

  List<VisitMemory> findByVisitIdIn(Collection<UUID> visitIds);

  Optional<VisitMemory> findByIdAndCustomerId(UUID id, UUID customerId);
}
