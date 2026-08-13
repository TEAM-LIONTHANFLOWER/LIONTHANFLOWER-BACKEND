// MySQL에서 초기 migration과 JPA 매핑의 호환성을 검증하는 테스트
package com.lionthanflower.infrastructure.persistence;

import com.lionthanflower.support.MySqlContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class InitialDomainMySqlMigrationTest extends MySqlContainerSupport {

  @Test
  void contextLoads() {}
}
