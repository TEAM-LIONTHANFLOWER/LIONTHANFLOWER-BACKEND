// 운영 health와 공개 Swagger endpoint 및 나머지 요청 보호를 검증하는 보안 테스트
package com.lionthanflower.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(CustomerApiSecurityConfigTest.TestController.class)
@Import({
  CustomerApiSecurityConfig.class,
  CustomerApiSecurityConfigTest.TestSecurityBeans.class,
  CustomerApiSecurityConfigTest.TestController.class
})
@TestPropertySource(
    properties =
        "app.cors.allowed-origins=http://localhost:8081,https://develop.mcm-orbit-n34.pages.dev,https://mcm-orbit-n34.pages.dev,https://mcm-orbit.site,https://api.mcm-orbit.site")
class CustomerApiSecurityConfigTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void health_endpoint는_인증_없이_접근할_수_있다() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void swagger_endpoint는_인증_없이_접근할_수_있다() throws Exception {
    mockMvc.perform(get("/swagger-ui.html")).andExpect(status().isOk());
    mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
  }

  @Test
  void 보호된_경로는_인증_없이_접근할_수_없다() throws Exception {
    mockMvc.perform(get("/private")).andExpect(status().isForbidden());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "http://localhost:8081",
        "https://develop.mcm-orbit-n34.pages.dev",
        "https://mcm-orbit-n34.pages.dev",
        "https://mcm-orbit.site",
        "https://api.mcm-orbit.site"
      })
  void 허용된_Origin의_API_응답에_CORS_헤더를_추가한다(String origin) throws Exception {
    mockMvc
        .perform(get("/api/customers/test").header(HttpHeaders.ORIGIN, origin))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
  }

  @Test
  void 허용된_Origin의_preflight_요청을_처리한다() throws Exception {
    mockMvc
        .perform(
            options("/api/customers/test")
                .header(HttpHeaders.ORIGIN, "http://localhost:8081")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:8081"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
  }

  @Test
  void 허용되지_않은_Origin의_API_요청을_차단한다() throws Exception {
    mockMvc
        .perform(get("/api/customers/test").header(HttpHeaders.ORIGIN, "https://evil.example"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void 허용되지_않은_Origin의_쿠키_포함_POST_요청을_차단한다() throws Exception {
    mockMvc
        .perform(
            post("/api/customers/test")
                .cookie(new jakarta.servlet.http.Cookie("customer_token", "known-token"))
                .header(HttpHeaders.ORIGIN, "https://evil.example"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
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

    @GetMapping("/api/customers/test")
    String customerApi() {
      return "customer";
    }

    @PostMapping("/api/customers/test")
    String updateCustomerApi() {
      return "updated";
    }

    @GetMapping("/swagger-ui.html")
    String swaggerUi() {
      return "swagger";
    }

    @GetMapping("/v3/api-docs")
    String apiDocs() {
      return "{}";
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
