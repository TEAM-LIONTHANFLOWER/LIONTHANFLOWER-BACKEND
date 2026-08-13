// API 오류 응답과 비즈니스 예외의 생성 규칙을 검증하는 테스트
package com.lionthanflower.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorResponseTest {

  @Test
  void 일반_오류_응답은_코드와_메시지와_빈_필드_오류를_제공한다() {
    ErrorResponse response = ErrorResponse.of(CommonErrorCode.INTERNAL_SERVER_ERROR);

    assertThat(response.success()).isFalse();
    assertThat(response.error().code()).isEqualTo("COMMON-500");
    assertThat(response.error().message()).isEqualTo("서버 내부 오류가 발생했습니다.");
    assertThat(response.error().fieldErrors()).isEmpty();
  }

  @Test
  void 비즈니스_예외는_오류_코드를_보존한다() {
    BusinessException exception = new BusinessException(CommonErrorCode.NOT_FOUND);

    assertThat(exception.errorCode()).isSameAs(CommonErrorCode.NOT_FOUND);
  }
}
