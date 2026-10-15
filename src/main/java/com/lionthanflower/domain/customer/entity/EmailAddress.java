// 이메일 계정과 인증 요청에서 사용하는 이메일 형식을 검증하는 클래스
package com.lionthanflower.domain.customer.entity;

import java.util.regex.Pattern;

public final class EmailAddress {
  private static final Pattern FORMAT =
      Pattern.compile(
          "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@"
              + "(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+"
              + "[A-Za-z]{2,63}");

  private EmailAddress() {}

  public static String validate(String email) {
    if (email == null
        || email.length() > 255
        || !FORMAT.matcher(email).matches()
        || email.indexOf('@') > 64) {
      throw new IllegalArgumentException("올바른 이메일 주소를 입력해 주세요.");
    }
    return email;
  }
}
