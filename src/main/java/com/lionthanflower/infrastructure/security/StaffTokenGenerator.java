// 직원 인증 토큰을 발급하고, 저장용 해시로 변환하는 컴포넌트
package com.lionthanflower.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class StaffTokenGenerator {
  private static final int TOKEN_BYTE_LENGTH = 32;
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  public String generateRawToken() {
    byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
    SECURE_RANDOM.nextBytes(randomBytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
  }

  public String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hasedBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hasedBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("토큰 해시 알고리즘을 사용할 수 없습니다.", e);
    }
  }
}
