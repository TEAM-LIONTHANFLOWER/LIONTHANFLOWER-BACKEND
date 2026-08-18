// 고객·직원 API의 익명 접근과 CSRF 예외를 제한적으로 설정하는 보안 구성
package com.lionthanflower.global.config;

import com.lionthanflower.domain.store.repository.StaffRepository;
import com.lionthanflower.infrastructure.security.StaffTokenAuthenticationFilter;
import com.lionthanflower.infrastructure.security.StaffTokenGenerator;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
      ObjectProvider<StaffTokenGenerator> staffTokenGeneratorProvider)
      throws Exception {

    http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/customers/**", "/api/staff/**"))
        .authorizeHttpRequests(
            auth ->
            auth.requestMatchers(
                    "/actuator/health",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/api/customers/**",
                    "/api/staff/**")
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
}
