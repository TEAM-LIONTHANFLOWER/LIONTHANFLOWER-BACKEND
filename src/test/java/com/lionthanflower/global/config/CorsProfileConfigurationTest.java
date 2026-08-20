// local·dev·prod 환경별 CORS 허용 Origin 설정을 검증하는 테스트
package com.lionthanflower.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

class CorsProfileConfigurationTest {

  @Test
  void local_기본_환경은_localhost_프론트만_허용한다() throws IOException {
    assertThat(corsAllowedOrigins("application.yml")).isEqualTo("http://localhost:8081");
  }

  @Test
  void dev_환경은_localhost와_develop_Pages_프론트를_허용한다() throws IOException {
    assertThat(corsAllowedOrigins("application-dev.yml"))
        .isEqualTo("http://localhost:8081,https://develop.mcm-orbit-n34.pages.dev");
  }

  @Test
  void prod_환경은_localhost와_develop과_운영_Pages_프론트를_허용한다() throws IOException {
    assertThat(corsAllowedOrigins("application-prod.yml"))
        .isEqualTo(
            "http://localhost:8081,https://develop.mcm-orbit-n34.pages.dev,https://mcm-orbit-n34.pages.dev");
  }

  private String corsAllowedOrigins(String resourceName) throws IOException {
    MutablePropertySources propertySources = new MutablePropertySources();
    new YamlPropertySourceLoader()
        .load(resourceName, new ClassPathResource(resourceName))
        .forEach(propertySources::addLast);
    return new PropertySourcesPropertyResolver(propertySources)
        .getProperty("app.cors.allowed-origins");
  }
}
