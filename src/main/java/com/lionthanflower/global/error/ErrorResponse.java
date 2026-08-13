// API 실패 응답의 공통 형식을 정의하는 객체
package com.lionthanflower.global.error;

import java.util.List;

public record ErrorResponse(boolean success, ErrorDetail error) {

  public static ErrorResponse of(ErrorCode errorCode) {
    return of(errorCode, List.of());
  }

  public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fieldErrors) {
    return new ErrorResponse(
        false, new ErrorDetail(errorCode.code(), errorCode.message(), List.copyOf(fieldErrors)));
  }

  public record ErrorDetail(String code, String message, List<FieldError> fieldErrors) {}

  public record FieldError(String field, String message) {}
}
