// API 오류의 HTTP 상태와 공개 메시지 계약을 정의하는 인터페이스
package com.lionthanflower.global.error;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

  HttpStatus status();

  String code();

  String message();
}
