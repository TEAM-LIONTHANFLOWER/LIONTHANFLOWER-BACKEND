// Arc 화면에 표시할 제품 정보 조회를 담당하는 Repository
package com.lionthanflower.infrastructure.persistence;

import com.lionthanflower.domain.product.entity.Product;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {}
