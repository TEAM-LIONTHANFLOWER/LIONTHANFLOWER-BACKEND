// 매장 엔티티의 데이터베이스 조회와 저장을 담당하는 Repository
package com.lionthanflower.infrastructure.persistence;

import com.lionthanflower.domain.store.entity.Store;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, UUID> {

  Optional<Store> findByCode(String code);

  List<Store> findTop20ByNameContainingIgnoreCaseOrCodeContainingIgnoreCaseOrderByNameAsc(
      String name, String code);
}
