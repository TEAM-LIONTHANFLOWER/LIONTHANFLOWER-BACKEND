// Spring Boot 애플리케이션 컨텍스트 로딩을 검증하는 테스트
package com.lionthanflower;

import com.lionthanflower.support.PostgreSqlContainerSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LionthanflowerBackendApplicationTests extends PostgreSqlContainerSupport {

  @Test
  void contextLoads() {}
}
