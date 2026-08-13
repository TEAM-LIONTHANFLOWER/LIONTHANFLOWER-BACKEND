// Arc 최종 이미지와 상태 전이 규칙을 검증하는 테스트
package com.lionthanflower.domain.arc.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArcTest {

  @Test
  void Arc는_DRAFT로_생성되고_수정_시_최종_이미지만_유지한다() {
    UUID createdBy = UUID.randomUUID();
    UUID modifiedBy = UUID.randomUUID();
    Arc arc = Arc.create(UUID.randomUUID(), UUID.randomUUID(), createdBy, "arc/draft-1.png");

    arc.replaceImage("arc/draft-2.png", modifiedBy);

    assertThat(arc.getStatus()).isEqualTo(ArcStatus.DRAFT);
    assertThat(arc.getImageObjectKey()).isEqualTo("arc/draft-2.png");
    assertThat(arc.getCreatedByStaffId()).isEqualTo(createdBy);
    assertThat(arc.getLastModifiedByStaffId()).isEqualTo(modifiedBy);
  }

  @Test
  void 확정된_Arc만_최종화할_수_있다() {
    Arc arc = Arc.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "arc/draft.png");

    assertThatThrownBy(() -> arc.finalizeArc(Instant.now()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("고객이 확정한 Arc만 최종 저장할 수 있습니다.");
  }

  @Test
  void 고객_확정_후_직원이_Arc를_최종화한다() {
    Arc arc = Arc.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "arc/final.png");
    Instant confirmedAt = Instant.parse("2026-08-13T16:00:00Z");
    Instant finalizedAt = Instant.parse("2026-08-13T16:05:00Z");

    arc.confirm(confirmedAt);
    arc.finalizeArc(finalizedAt);

    assertThat(arc.getStatus()).isEqualTo(ArcStatus.FINALIZED);
    assertThat(arc.getConfirmedAt()).isEqualTo(confirmedAt);
    assertThat(arc.getFinalizedAt()).isEqualTo(finalizedAt);
  }
}
