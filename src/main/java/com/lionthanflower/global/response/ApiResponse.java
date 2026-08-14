// API 성공 응답의 공통 형식을 정의하는 객체
package com.lionthanflower.global.response;

public record ApiResponse<T>(boolean success, T data) {

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, data);
  }
}
