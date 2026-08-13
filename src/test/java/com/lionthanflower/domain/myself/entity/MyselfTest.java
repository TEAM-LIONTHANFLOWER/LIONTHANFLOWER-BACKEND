// Myself 이미지 생성 작업의 상태 전이를 검증하는 테스트
package com.lionthanflower.domain.myself.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MyselfTest {

  @Test
  void 생성_작업은_PROCESSING으로_시작한다() {
    Myself myself =
        Myself.create(UUID.randomUUID(), UUID.randomUUID(), FrameType.FRAME_1, "myself/source.png");

    assertThat(myself.getStatus()).isEqualTo(MyselfStatus.PROCESSING);
    assertThat(myself.getResultImageObjectKey()).isNull();
  }

  @Test
  void 생성_완료_시_결과_이미지와_완료_시각을_저장한다() {
    Myself myself =
        Myself.create(UUID.randomUUID(), UUID.randomUUID(), FrameType.FRAME_2, "myself/source.png");
    Instant completedAt = Instant.parse("2026-08-13T17:00:00Z");

    myself.complete("myself/result.png", completedAt);
    myself.clearSourceImage();

    assertThat(myself.getStatus()).isEqualTo(MyselfStatus.COMPLETED);
    assertThat(myself.getResultImageObjectKey()).isEqualTo("myself/result.png");
    assertThat(myself.getSourceImageObjectKey()).isNull();
    assertThat(myself.getCompletedAt()).isEqualTo(completedAt);
  }

  @Test
  void 종료된_작업의_상태를_다시_변경할_수_없다() {
    Myself myself =
        Myself.create(UUID.randomUUID(), UUID.randomUUID(), FrameType.FRAME_1, "myself/source.png");
    myself.fail("OPENAI_UNAVAILABLE", Instant.parse("2026-08-13T17:00:00Z"));

    assertThatThrownBy(
            () -> myself.complete("myself/result.png", Instant.parse("2026-08-13T17:05:00Z")))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("처리 중인 Myself 작업만 완료할 수 있습니다.");
  }
}
