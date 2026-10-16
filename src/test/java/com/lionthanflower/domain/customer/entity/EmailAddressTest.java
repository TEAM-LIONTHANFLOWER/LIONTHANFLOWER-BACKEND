// 이메일 형식과 계정 및 인증 요청의 입력 검증을 확인하는 테스트
package com.lionthanflower.domain.customer.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EmailAddressTest {
  @ParameterizedTest
  @ValueSource(strings = {"user@example.com", "first.last+tag@example.co.kr"})
  void 유효한_이메일로_계정과_인증요청을_생성한다(String email) {
    assertThat(CustomerEmailIdentity.create(email).getEmail()).isEqualTo(email);
    assertThat(
            EmailVerificationRequest.create(
                    UUID.randomUUID(),
                    email,
                    "ARCHIVE",
                    "hash",
                    Instant.parse("2030-01-01T00:00:00Z"))
                .getEmail())
        .isEqualTo(email);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(
      strings = {
        " ",
        "user",
        "user@",
        "@example.com",
        "user@@example.com",
        "user@example",
        "user name@example.com",
        " user@example.com",
        "user@example.com ",
        ".user@example.com",
        "user..name@example.com",
        "user@-example.com",
        "user@example-.com"
      })
  void 잘못된_이메일은_계정과_인증요청에서_거절한다(String email) {
    assertThatThrownBy(() -> CustomerEmailIdentity.create(email))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                EmailVerificationRequest.create(
                    UUID.randomUUID(),
                    email,
                    "ARCHIVE",
                    "hash",
                    Instant.parse("2030-01-01T00:00:00Z")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"User@Example.COM", "User@example.com", "User@EXAMPLE.com"})
  void 계정과_인증요청은_도메인만_소문자로_정규화한다(String email) {
    assertThat(CustomerEmailIdentity.create(email).getEmail()).isEqualTo("User@example.com");
    assertThat(
            EmailVerificationRequest.create(
                    UUID.randomUUID(),
                    email,
                    "ARCHIVE",
                    "hash",
                    Instant.parse("2030-01-01T00:00:00Z"))
                .getEmail())
        .isEqualTo("User@example.com");
    assertThat(EmailAddress.normalize(EmailAddress.normalize(email))).isEqualTo("User@example.com");
  }

  @Test
  void 로컬파트와_전체_길이_제한을_검증한다() {
    assertThat(EmailAddress.validate("a".repeat(64) + "@example.com")).isNotNull();
    assertThatThrownBy(() -> EmailAddress.validate("a".repeat(65) + "@example.com"))
        .isInstanceOf(IllegalArgumentException.class);
    String domain = "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
    assertThat(EmailAddress.validate("a".repeat(64) + "@" + domain)).hasSize(254);
    assertThatThrownBy(() -> EmailAddress.validate("a".repeat(64) + "@" + domain + "ee"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
