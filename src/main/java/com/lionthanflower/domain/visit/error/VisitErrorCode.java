// 방문 조회와 상태 전이 실패를 API 오류로 표현하는 열거형
package com.lionthanflower.domain.visit.error;

import com.lionthanflower.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum VisitErrorCode implements ErrorCode {
  NOT_FOUND(HttpStatus.NOT_FOUND, "VISIT-404", "방문을 찾을 수 없습니다."),
  NOT_ASSIGNABLE(HttpStatus.CONFLICT, "VISIT-409", "응대를 시작할 수 없는 방문입니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  VisitErrorCode(HttpStatus status, String code, String message) {
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
