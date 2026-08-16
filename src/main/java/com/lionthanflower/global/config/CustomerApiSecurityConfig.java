// 고객 API의 익명 접근과 CSRF 예외를 제한적으로 설정하는 보안 구성
package com.lionthanflower.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(
    name = "app.customer-api-security.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class CustomerApiSecurityConfig {

  @Bean
  SecurityFilterChain customerApiSecurityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/customers/**"))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/customers/**").permitAll().anyRequest().authenticated())
        .build();
  }
}
