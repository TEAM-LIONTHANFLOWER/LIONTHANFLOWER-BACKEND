// 정적 프레임 카탈로그에서 공개 목록을 만드는 서비스
package com.lionthanflower.application.studio;

import com.lionthanflower.domain.myself.entity.FrameType;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StudioFrameService {

  private final StudioFrameCatalog catalog;

  public StudioFrameService(StudioFrameCatalog catalog) {
    this.catalog = catalog;
  }

  public List<StudioFrame> getFrames() {
    return catalog.frames().stream()
        .map(
            frame ->
                new StudioFrame(frame.frameType(), frame.displayName(), frame.overlayImageUrl()))
        .toList();
  }

  public record StudioFrame(FrameType frameType, String displayName, String overlayImageUrl) {}
}
