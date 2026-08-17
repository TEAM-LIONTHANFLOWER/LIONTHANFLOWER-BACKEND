// Arc의 활성 리비전 조회와 저장을 담당하는 Repository
package com.lionthanflower.infrastructure.persistence;

import com.lionthanflower.domain.arc.entity.ArcRevision;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArcRevisionRepository extends JpaRepository<ArcRevision, UUID> {}
