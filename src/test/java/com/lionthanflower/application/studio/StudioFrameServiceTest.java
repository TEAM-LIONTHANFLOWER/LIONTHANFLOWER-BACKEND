// MCM Studio 프레임 카탈로그 조회 규칙을 검증하는 테스트
package com.lionthanflower.application.studio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.junit.jupiter.api.Test;

class StudioFrameServiceTest {

  @Test
  void 네_개_프레임을_파일명_상대_경로와_함께_카탈로그_순서로_반환한다() {
    StudioFrameService service = new StudioFrameService(new StudioFrameCatalog());

    assertThat(service.getFrames())
        .extracting(
            frame -> frame.frameType().name(),
            StudioFrameService.StudioFrame::displayName,
            StudioFrameService.StudioFrame::overlayImageUrl)
        .containsExactly(
            tuple("FRAME_1", "MCM Frame 1", "/mcm-studio/Frame_1.png"),
            tuple("FRAME_2", "MCM Frame 2", "/mcm-studio/Frame_2.png"),
            tuple("FRAME_3", "MCM Frame 3", "/mcm-studio/Frame_3.png"),
            tuple("FRAME_4", "MCM Frame 4", "/mcm-studio/Frame_4.png"));
  }
}
