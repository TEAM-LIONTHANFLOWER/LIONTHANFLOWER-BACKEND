// 직원 Arc 생성과 공유 API에서 사용하는 오류 코드를 정의하는 열거형
package com.lionthanflower.domain.arc.error;

import com.lionthanflower.global.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ArcErrorCode implements ErrorCode {
  NOT_FOUND(HttpStatus.NOT_FOUND, "ARC-404", "Arc를 찾을 수 없습니다."),
  NOT_ASSIGNABLE(HttpStatus.CONFLICT, "ARC-409", "Arc를 생성하거나 공유할 수 없습니다."),
  REVISION_NOT_READY(HttpStatus.CONFLICT, "ARC-409", "생성이 완료된 Arc만 공유할 수 있습니다."),
  ALREADY_EXISTS(HttpStatus.CONFLICT, "ARC-409", "해당 방문의 Arc가 이미 존재합니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  ArcErrorCode(HttpStatus status, String code, String message) {
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
