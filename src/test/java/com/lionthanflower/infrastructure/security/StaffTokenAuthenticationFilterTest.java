// 직원 토큰 인증 필터의 인증 주체 보존 규칙을 검증하는 테스트
package com.lionthanflower.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.store.repository.StaffRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class StaffTokenAuthenticationFilterTest {

  @Mock private StaffRepository staffRepository;
  @Mock private StaffTokenGenerator staffTokenGenerator;
  @Mock private FilterChain filterChain;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void 기존_인증이_있으면_staffToken으로_인증을_덮어쓰지_않는다() throws ServletException, IOException {
    Authentication existing =
        new UsernamePasswordAuthenticationToken("existing-principal", null, List.of());
    SecurityContextHolder.getContext().setAuthentication(existing);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new jakarta.servlet.http.Cookie("staffToken", "staff-token"));

    new StaffTokenAuthenticationFilter(staffRepository, staffTokenGenerator)
        .doFilter(request, new MockHttpServletResponse(), filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
    verifyNoInteractions(staffRepository, staffTokenGenerator);
  }

  @Test
  void 인증이_없고_유효한_staffToken이_있으면_직원을_인증한다() throws ServletException, IOException {
    Staff staff =
        Staff.create(
            java.util.UUID.randomUUID(),
            "김형진",
            "staff-hash",
            Set.of(com.lionthanflower.domain.common.entity.LanguageCode.EN));
    when(staffTokenGenerator.hash("staff-token")).thenReturn("staff-hash");
    when(staffRepository.findByTokenHash("staff-hash")).thenReturn(java.util.Optional.of(staff));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setCookies(new jakarta.servlet.http.Cookie("staffToken", "staff-token"));

    new StaffTokenAuthenticationFilter(staffRepository, staffTokenGenerator)
        .doFilter(request, new MockHttpServletResponse(), filterChain);

    assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
        .isSameAs(staff);
  }
}
