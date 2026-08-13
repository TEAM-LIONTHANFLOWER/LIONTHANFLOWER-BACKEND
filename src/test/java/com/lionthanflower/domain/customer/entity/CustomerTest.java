// 익명 고객의 생성과 이름 입력 규칙을 검증하는 테스트
package com.lionthanflower.domain.customer.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CustomerTest {

  @Test
  void 익명_고객은_이름_없이_토큰_해시로_생성된다() {
    Customer customer = Customer.create("customer-token-hash");

    assertThat(customer.getId()).isNotNull();
    assertThat(customer.getName()).isNull();
    assertThat(customer.getTokenHash()).isEqualTo("customer-token-hash");
  }

  @Test
  void 온보딩에서_고객_이름을_입력한다() {
    Customer customer = Customer.create("customer-token-hash");

    customer.updateName("홍길동");

    assertThat(customer.getName()).isEqualTo("홍길동");
  }

  @Test
  void 고객_이름은_공백일_수_없다() {
    Customer customer = Customer.create("customer-token-hash");

    assertThatThrownBy(() -> customer.updateName(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("고객 이름은 비어 있을 수 없습니다.");
  }
}
