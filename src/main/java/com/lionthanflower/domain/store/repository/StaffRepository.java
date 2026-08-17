// 직원 프로필 조회,저장하는 Spring Data JPA 저장소
package com.lionthanflower.domain.store.repository;

import com.lionthanflower.domain.store.entity.Staff;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffRepository extends JpaRepository<Staff, UUID> {

  Optional<Staff> findByTokenHash(String tokenHash);

  boolean existsByTokenHash(String tokenHash);
}
