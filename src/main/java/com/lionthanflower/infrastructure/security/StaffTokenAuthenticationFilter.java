// staffToken 쿠키를 확인해 인증된 Staff를 SecurityContext에 설정하는 필터
package com.lionthanflower.infrastructure.security;

import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.store.repository.StaffRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class StaffTokenAuthenticationFilter extends OncePerRequestFilter {

  private static final String STAFF_TOKEN_COOKIE_NAME = "staffToken";

  private final StaffRepository staffRepository;
  private final StaffTokenGenerator staffTokenGenerator;

  public StaffTokenAuthenticationFilter(
      StaffRepository staffRepository, StaffTokenGenerator staffTokenGenerator) {
    this.staffRepository = staffRepository;
    this.staffTokenGenerator = staffTokenGenerator;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      findRawToken(request)
          .map(staffTokenGenerator::hash)
          .flatMap(staffRepository::findByTokenHash)
          .ifPresent(this::setAuthentication);
    }

    filterChain.doFilter(request, response);
  }

  private Optional<String> findRawToken(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return Optional.empty();
    }
    return List.of(cookies).stream()
        .filter(cookie -> STAFF_TOKEN_COOKIE_NAME.equals(cookie.getName()))
        .map(Cookie::getValue)
        .findFirst();
  }

  private void setAuthentication(Staff staff) {
    var authentication = new UsernamePasswordAuthenticationToken(staff, null, List.of());
    SecurityContextHolder.getContext().setAuthentication(authentication);
  }
}
