// MCM Studio에서 제공하는 프레임의 식별자, 표시 이름과 순서를 정의하는 카탈로그
package com.lionthanflower.application.studio;

import com.lionthanflower.domain.myself.entity.FrameType;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StudioFrameCatalog {

  private static final List<FrameDefinition> FRAMES =
      List.of(
          new FrameDefinition(FrameType.FRAME_1, "MCM Frame 1", "/mcm-studio/Frame_1.png"),
          new FrameDefinition(FrameType.FRAME_2, "MCM Frame 2", "/mcm-studio/Frame_2.png"),
          new FrameDefinition(FrameType.FRAME_3, "MCM Frame 3", "/mcm-studio/Frame_3.png"),
          new FrameDefinition(FrameType.FRAME_4, "MCM Frame 4", "/mcm-studio/Frame_4.png"));

  public List<FrameDefinition> frames() {
    return FRAMES;
  }

  public record FrameDefinition(FrameType frameType, String displayName, String overlayImageUrl) {}
}
