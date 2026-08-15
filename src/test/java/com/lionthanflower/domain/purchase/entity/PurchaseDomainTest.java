// 한 번의 방문에서 복수 제품을 구매하는 규칙을 검증하는 테스트
package com.lionthanflower.domain.purchase.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PurchaseDomainTest {

  @Test
  void 한_방문의_복수_구매_항목을_생성한다() {
    Purchase purchase = Purchase.create(UUID.randomUUID());
    List<PurchaseItem> items =
        PurchaseItem.createAll(purchase.getId(), List.of(UUID.randomUUID(), UUID.randomUUID()));

    assertThat(items).hasSize(2);
    assertThat(items).allMatch(item -> item.getPurchaseId().equals(purchase.getId()));
  }

  @Test
  void 구매에는_하나_이상의_제품이_필요하다() {
    assertThatThrownBy(() -> PurchaseItem.createAll(UUID.randomUUID(), List.of()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("구매 제품은 하나 이상이어야 합니다.");
  }

  @Test
  void 같은_Variant를_중복_선택할_수_없다() {
    UUID variantId = UUID.randomUUID();

    assertThatThrownBy(
            () -> PurchaseItem.createAll(UUID.randomUUID(), List.of(variantId, variantId)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("같은 제품 Variant를 중복 선택할 수 없습니다.");
  }

  @Test
  void 구매에는_방문_ID가_필요하다() {
    assertThatThrownBy(() -> Purchase.create(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("방문 ID는 null일 수 없습니다.");
  }

  @Test
  void 구매_제품에는_null_Variant_ID가_포함될_수_없다() {
    assertThatThrownBy(
            () -> PurchaseItem.createAll(UUID.randomUUID(), Arrays.asList(UUID.randomUUID(), null)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("제품 Variant ID는 null일 수 없습니다.");
  }
}
