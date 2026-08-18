// 직원 Visit Memory API에서 사용하는 오류 코드를 정의하는 열거형
package com.lionthanflower.domain.visitmemory.error;

import com.lionthanflower.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum VisitMemoryErrorCode implements ErrorCode {
  NOT_FOUND(HttpStatus.NOT_FOUND, "VISIT-MEMORY-404", "Visit Memory를 찾을 수 없습니다."),
  NOT_ASSIGNABLE(HttpStatus.CONFLICT, "VISIT-MEMORY-409", "Visit Memory를 처리할 수 없습니다."),
  NOT_READY(HttpStatus.CONFLICT, "VISIT-MEMORY-409", "생성이 완료된 Visit Memory만 전송할 수 있습니다."),
  ALREADY_EXISTS(HttpStatus.CONFLICT, "VISIT-MEMORY-409", "해당 방문의 Visit Memory가 이미 존재합니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  VisitMemoryErrorCode(HttpStatus status, String code, String message) {
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
