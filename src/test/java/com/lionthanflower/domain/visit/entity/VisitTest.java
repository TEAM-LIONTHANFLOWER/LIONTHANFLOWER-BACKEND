// 방문 온보딩과 직원 매칭 상태 전이를 검증하는 테스트
package com.lionthanflower.domain.visit.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VisitTest {

  private final UUID customerId = UUID.randomUUID();
  private final UUID storeId = UUID.randomUUID();

  @Test
  void 직원_추천_온보딩은_대기_상태가_된다() {
    Visit visit = Visit.create(customerId, storeId, "A-001");

    visit.completeOnboarding(
        ServiceLanguage.KO, InteractionStyle.STAFF_RECOMMENDATION, "신상품을 보고 싶어요");

    assertThat(visit.getStatus()).isEqualTo(VisitStatus.WAITING_FOR_STAFF);
  }

  @Test
  void 혼자_보기_온보딩은_SELF_GUIDED_상태가_된다() {
    Visit visit = Visit.create(customerId, storeId, "A-002");

    visit.completeOnboarding(ServiceLanguage.JA, InteractionStyle.SELF_GUIDED, null);

    assertThat(visit.getStatus()).isEqualTo(VisitStatus.SELF_GUIDED);
  }

  @Test
  void 직원_추천_고객을_선택하면_MATCHED_상태가_된다() {
    Visit visit = Visit.create(customerId, storeId, "A-003");
    Instant matchedAt = Instant.parse("2026-08-13T13:00:00Z");
    visit.completeOnboarding(ServiceLanguage.KO, InteractionStyle.STAFF_RECOMMENDATION, null);

    visit.matchForRecommendation(UUID.randomUUID(), UUID.randomUUID(), matchedAt);

    assertThat(visit.getStatus()).isEqualTo(VisitStatus.MATCHED);
    assertThat(visit.getMatchedAt()).isEqualTo(matchedAt);
  }

  @Test
  void 혼자_보기_고객을_구매_후_선택하면_Arc_진행_상태가_된다() {
    Visit visit = Visit.create(customerId, storeId, "A-004");
    Instant grantedAt = Instant.parse("2026-08-13T14:00:00Z");
    visit.completeOnboarding(ServiceLanguage.ZH, InteractionStyle.SELF_GUIDED, null);

    visit.startArcForSelfGuided(UUID.randomUUID(), UUID.randomUUID(), grantedAt);

    assertThat(visit.getStatus()).isEqualTo(VisitStatus.ARC_IN_PROGRESS);
    assertThat(visit.getArcCreationGrantedAt()).isEqualTo(grantedAt);
    assertThat(visit.isArcCreationGranted()).isTrue();
  }

  @Test
  void 직원_추천_고객은_매칭_후에만_Arc_권한을_받을_수_있다() {
    Visit visit = Visit.create(customerId, storeId, "A-005");
    visit.completeOnboarding(ServiceLanguage.RU, InteractionStyle.STAFF_RECOMMENDATION, null);

    assertThatThrownBy(() -> visit.grantArcCreation(Instant.now()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("매칭된 방문만 Arc 생성 권한을 받을 수 있습니다.");
  }

  @Test
  void 매칭된_직원_추천_고객에게_Arc_권한을_부여한다() {
    Visit visit = Visit.create(customerId, storeId, "A-006");
    Instant grantedAt = Instant.parse("2026-08-13T14:30:00Z");
    visit.completeOnboarding(ServiceLanguage.KO, InteractionStyle.STAFF_RECOMMENDATION, null);
    visit.matchForRecommendation(UUID.randomUUID(), UUID.randomUUID(), grantedAt.minusSeconds(60));

    visit.grantArcCreation(grantedAt);

    assertThat(visit.getStatus()).isEqualTo(VisitStatus.ARC_IN_PROGRESS);
    assertThat(visit.getArcCreationGrantedAt()).isEqualTo(grantedAt);
  }

  @Test
  void 진행_중인_방문을_취소하면_종료_상태가_된다() {
    Visit visit = Visit.create(customerId, storeId, "A-007");
    Instant canceledAt = Instant.parse("2026-08-13T14:40:00Z");
    visit.completeOnboarding(ServiceLanguage.KO, InteractionStyle.SELF_GUIDED, null);

    visit.cancel(canceledAt);

    assertThat(visit.getStatus()).isEqualTo(VisitStatus.CANCELED);
    assertThat(visit.getCanceledAt()).isEqualTo(canceledAt);
  }

  @Test
  void 종료된_방문은_다시_매칭할_수_없다() {
    Visit visit = Visit.create(customerId, storeId, "A-008");
    visit.completeOnboarding(ServiceLanguage.KO, InteractionStyle.SELF_GUIDED, null);
    visit.completeWithoutPurchase(Instant.parse("2026-08-13T15:00:00Z"));

    assertThatThrownBy(
            () -> visit.startArcForSelfGuided(UUID.randomUUID(), UUID.randomUUID(), Instant.now()))
        .isInstanceOf(IllegalStateException.class);
  }
}
