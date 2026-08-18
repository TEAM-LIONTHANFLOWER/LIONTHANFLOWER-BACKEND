// 운영 health endpoint 공개와 나머지 요청 보호를 검증하는 보안 테스트
package com.lionthanflower.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(CustomerApiSecurityConfigTest.TestController.class)
@Import({
  CustomerApiSecurityConfig.class,
  CustomerApiSecurityConfigTest.TestSecurityBeans.class,
  CustomerApiSecurityConfigTest.TestController.class
})
class CustomerApiSecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void health_endpoint는_인증_없이_접근할_수_있다() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void 보호된_경로는_인증_없이_접근할_수_없다() throws Exception {
    mockMvc.perform(get("/private")).andExpect(status().isForbidden());
  }

  @RestController
  static class TestController {
    @GetMapping("/actuator/health")
    String health() {
      return "{\"status\":\"UP\"}";
    }

    @GetMapping("/private")
    String privateEndpoint() {
      return "private";
    }
  }

  @TestConfiguration
  static class TestSecurityBeans {
    @Bean
    InMemoryUserDetailsManager userDetailsService() {
      return new InMemoryUserDetailsManager(
          User.withUsername("test").password("{noop}test").roles("USER").build());
    }
  }
}
