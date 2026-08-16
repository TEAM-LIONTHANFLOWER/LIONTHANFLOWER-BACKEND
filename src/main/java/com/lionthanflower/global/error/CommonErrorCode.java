// 애플리케이션 전역에서 사용하는 공통 API 오류 코드를 정의하는 열거형
package com.lionthanflower.global.error;

import org.springframework.http.HttpStatus;

public enum CommonErrorCode implements ErrorCode {
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-400", "요청 값이 올바르지 않습니다."),
  CONFLICT(HttpStatus.CONFLICT, "COMMON-409", "요청한 리소스가 다른 요청에 의해 변경되었습니다."),
  NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON-404", "요청한 리소스를 찾을 수 없습니다."),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON-405", "지원하지 않는 HTTP 메서드입니다."),
  UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMON-415", "지원하지 않는 미디어 타입입니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-500", "서버 내부 오류가 발생했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  CommonErrorCode(HttpStatus status, String code, String message) {
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
