// 고객 익명 토큰의 생성과 해시 변환 규칙을 검증하는 테스트
package com.lionthanflower.application.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CustomerTokenManagerTest {

  private final CustomerTokenManager customerTokenManager = new CustomerTokenManager();

  @Test
  void 예측하기_어려운_서로_다른_고객_토큰을_생성한다() {
    String firstToken = customerTokenManager.generate();
    String secondToken = customerTokenManager.generate();

    assertThat(firstToken).isNotBlank().isNotEqualTo(secondToken);
    assertThat(secondToken).isNotBlank();
  }

  @Test
  void 같은_원본_토큰을_동일한_SHA_256_해시로_변환한다() {
    String firstHash = customerTokenManager.hash("customer-token");
    String secondHash = customerTokenManager.hash("customer-token");

    assertThat(firstHash).isEqualTo(secondHash).hasSize(64);
  }

  @Test
  void 빈_원본_토큰은_해시로_변환하지_않는다() {
    assertThatThrownBy(() -> customerTokenManager.hash(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("고객 토큰은 비어 있을 수 없습니다.");
  }
}
