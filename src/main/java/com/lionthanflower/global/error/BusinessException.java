// 비즈니스 오류 코드를 전달하는 애플리케이션 공통 예외
package com.lionthanflower.global.error;

public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;

  public BusinessException(ErrorCode errorCode) {
    super(errorCode.message());
    this.errorCode = errorCode;
  }

  public ErrorCode errorCode() {
    return errorCode;
  }
}
