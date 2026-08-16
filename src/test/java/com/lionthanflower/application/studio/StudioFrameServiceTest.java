// MCM Studio 프레임 카탈로그 조회 규칙을 검증하는 테스트
package com.lionthanflower.application.studio;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionthanflower.domain.myself.entity.FrameType;
import com.lionthanflower.global.config.StudioFrameProperties;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StudioFrameServiceTest {

  @Test
  void 설정된_URL이_없으면_빈_목록을_반환한다() {
    StudioFrameService service = serviceWith(Map.of());

    assertThat(service.getFrames()).isEmpty();
  }

  @Test
  void URL이_설정된_프레임만_반환한다() {
    StudioFrameService service =
        serviceWith(Map.of(FrameType.FRAME_2, "https://cdn.example.com/frame-2/overlay.png"));

    assertThat(service.getFrames())
        .containsExactly(
            new StudioFrameService.StudioFrame(
                FrameType.FRAME_2, "MCM Frame 2", "https://cdn.example.com/frame-2/overlay.png"));
  }

  @Test
  void 모든_URL이_설정되면_정적_카탈로그_순서로_반환한다() {
    StudioFrameService service =
        serviceWith(
            Map.of(
                FrameType.FRAME_2, "https://cdn.example.com/frame-2/overlay.png",
                FrameType.FRAME_1, "https://cdn.example.com/frame-1/overlay.png"));

    assertThat(service.getFrames())
        .containsExactly(
            new StudioFrameService.StudioFrame(
                FrameType.FRAME_1, "MCM Frame 1", "https://cdn.example.com/frame-1/overlay.png"),
            new StudioFrameService.StudioFrame(
                FrameType.FRAME_2, "MCM Frame 2", "https://cdn.example.com/frame-2/overlay.png"));
  }

  private StudioFrameService serviceWith(Map<FrameType, String> urls) {
    return new StudioFrameService(new StudioFrameCatalog(), new StudioFrameProperties(urls));
  }
}
