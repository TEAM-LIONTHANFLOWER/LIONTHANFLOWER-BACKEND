// 방문 온보딩과 구매 판단 상태 전이를 검증하는 테스트
package com.lionthanflower.domain.visit.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionthanflower.domain.common.entity.LanguageCode;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VisitTest {

  private final UUID customerId = UUID.randomUUID();
  private final UUID storeId = UUID.randomUUID();

  @Test
  void 직원_추천_온보딩과_매칭을_진행한다() {
    Visit visit = Visit.create(customerId, storeId);
    Instant matchedAt = Instant.parse("2026-08-15T10:00:00Z");
    UUID staffId = UUID.randomUUID();

    visit.completeOnboarding(LanguageCode.EN, InteractionStyle.STAFF_RECOMMENDATION, "신상품을 보고 싶어요");
    assertThat(visit.getStatus()).isEqualTo(VisitStatus.WAITING_FOR_STAFF);

    visit.assignStaff(staffId, matchedAt);
    assertThat(visit.getStatus()).isEqualTo(VisitStatus.ACTIVE);
    assertThat(visit.getStaffId()).isEqualTo(staffId);
  }

  @Test
  void 혼자_보기_온보딩은_ACTIVE_상태가_된다() {
    Visit visit = Visit.create(customerId, storeId);

    visit.completeOnboarding(LanguageCode.JA, InteractionStyle.SELF_GUIDED, null);

    assertThat(visit.getStatus()).isEqualTo(VisitStatus.ACTIVE);
  }

  @Test
  void 구매_확정은_Arc_진행_상태로_전환한다() {
    Visit visit = activeVisit();
    UUID staffId = visit.getStaffId();
    Instant decidedAt = Instant.parse("2026-08-15T11:00:00Z");

    visit.confirmPurchase(staffId, decidedAt);

    assertThat(visit.getPurchaseDecision()).isEqualTo(PurchaseDecision.PURCHASED);
    assertThat(visit.getPurchaseDecidedByStaffId()).isEqualTo(staffId);
    assertThat(visit.getPurchaseDecidedAt()).isEqualTo(decidedAt);
    assertThat(visit.getStatus()).isEqualTo(VisitStatus.ARC_IN_PROGRESS);
  }

  @Test
  void 미구매_확정은_Visit_Memory_진행_상태로_전환한다() {
    Visit visit = activeVisit();
    Instant decidedAt = Instant.parse("2026-08-15T11:30:00Z");

    visit.confirmNoPurchase(visit.getStaffId(), decidedAt);

    assertThat(visit.getPurchaseDecision()).isEqualTo(PurchaseDecision.NOT_PURCHASED);
    assertThat(visit.getStatus()).isEqualTo(VisitStatus.VISIT_MEMORY_IN_PROGRESS);
  }

  @Test
  void 담당자가_아닌_직원은_구매를_확정할_수_없다() {
    Visit visit = activeVisit();

    assertThatThrownBy(() -> visit.confirmPurchase(UUID.randomUUID(), Instant.now()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("담당 직원만 구매 여부를 확정할 수 있습니다.");
  }

  @Test
  void Arc_또는_Visit_Memory_진행_중인_방문을_완료한다() {
    Visit purchasedVisit = activeVisit();
    purchasedVisit.confirmPurchase(purchasedVisit.getStaffId(), Instant.now());
    purchasedVisit.complete(Instant.parse("2026-08-15T12:00:00Z"));

    Visit notPurchasedVisit = activeVisit();
    notPurchasedVisit.confirmNoPurchase(notPurchasedVisit.getStaffId(), Instant.now());
    notPurchasedVisit.complete(Instant.parse("2026-08-15T12:30:00Z"));

    assertThat(purchasedVisit.getStatus()).isEqualTo(VisitStatus.COMPLETED);
    assertThat(notPurchasedVisit.getStatus()).isEqualTo(VisitStatus.COMPLETED);
  }

  @Test
  void 구매_여부가_확정되지_않은_방문은_완료할_수_없다() {
    Visit visit = activeVisit();

    assertThatThrownBy(() -> visit.complete(Instant.now()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Arc 또는 Visit Memory 진행 중인 방문만 종료할 수 있습니다.");
  }

  @Test
  void 진행_중인_방문을_취소한다() {
    Visit visit = activeVisit();
    Instant canceledAt = Instant.parse("2026-08-15T13:00:00Z");

    visit.cancel(canceledAt);

    assertThat(visit.getStatus()).isEqualTo(VisitStatus.CANCELED);
    assertThat(visit.getCanceledAt()).isEqualTo(canceledAt);
  }

  private Visit activeVisit() {
    Visit visit = Visit.create(customerId, storeId);
    visit.completeOnboarding(LanguageCode.EN, InteractionStyle.SELF_GUIDED, null);
    visit.assignStaff(UUID.randomUUID(), Instant.parse("2026-08-15T10:00:00Z"));
    return visit;
  }
}
