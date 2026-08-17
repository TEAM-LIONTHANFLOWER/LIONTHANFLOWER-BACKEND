// 직원 프로필 등록 API에서 사용하는 오류코드 정의하는 열거형
package com.lionthanflower.domain.store.error;

import com.lionthanflower.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum StaffErrorCode implements ErrorCode {
  INVALID_STORE_ID(HttpStatus.BAD_REQUEST, "STAFF-400-1", "존재하지 않는 매장 ID입니다."),
  INVALID_LANGUAGE_CODE(HttpStatus.BAD_REQUEST, "STAFF-400-2", "지원하지 않는 언어 코드가 포함되어 있습니다."),
  PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "STAFF-409", "이미 프로필이 등록된 직원입니다."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "STAFF-401", "인증되지 않은 요청입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  StaffErrorCode(HttpStatus status, String code, String message) {
    this.status = status;
    this.code = code;
    this.message = message;
  }

  @Override
  public HttpStatus status() {
    return status;
  }

  @Override
  public String code() {
    return code;
  }

  @Override
  public String message() {
    return message;
  }
}
