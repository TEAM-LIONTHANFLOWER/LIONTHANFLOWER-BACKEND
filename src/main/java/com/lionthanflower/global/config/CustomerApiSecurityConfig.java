// 고객·직원 API의 익명 접근과 CSRF 예외를 제한적으로 설정하는 보안 구성
package com.lionthanflower.global.config;

import com.lionthanflower.domain.store.repository.StaffRepository;
import com.lionthanflower.infrastructure.security.StaffTokenAuthenticationFilter;
import com.lionthanflower.infrastructure.security.StaffTokenGenerator;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(
    name = "app.customer-api-security.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class CustomerApiSecurityConfig {

  @Bean
  SecurityFilterChain customerApiSecurityFilterChain(
      HttpSecurity http,
      ObjectProvider<StaffRepository> staffRepositoryProvider,
      ObjectProvider<StaffTokenGenerator> staffTokenGeneratorProvider,
      CorsConfigurationSource corsConfigurationSource)
      throws Exception {

    http.cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/customers/**", "/api/staff/**"))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/actuator/health", "/api/customers/**", "/api/staff/**", "/api/stores")
                    .permitAll()
                    .anyRequest()
                    .authenticated());

    StaffRepository staffRepository = staffRepositoryProvider.getIfAvailable();
    StaffTokenGenerator staffTokenGenerator = staffTokenGeneratorProvider.getIfAvailable();
    if (staffRepository != null && staffTokenGenerator != null) {
      http.addFilterBefore(
          new StaffTokenAuthenticationFilter(staffRepository, staffTokenGenerator),
          UsernamePasswordAuthenticationFilter.class);
    }

    return http.build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins:http://localhost:8081}") String allowedOrigins) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .toList());
    configuration.setAllowedMethods(
        List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }
}
