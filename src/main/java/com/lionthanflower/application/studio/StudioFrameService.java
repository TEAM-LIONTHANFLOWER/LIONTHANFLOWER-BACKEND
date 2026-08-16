// 정적 프레임 카탈로그와 에셋 URL 설정을 결합해 공개 목록을 만드는 서비스
package com.lionthanflower.application.studio;

import com.lionthanflower.domain.myself.entity.FrameType;
import com.lionthanflower.global.config.StudioFrameProperties;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StudioFrameService {

  private final StudioFrameCatalog catalog;
  private final StudioFrameProperties properties;

  public StudioFrameService(StudioFrameCatalog catalog, StudioFrameProperties properties) {
    this.catalog = catalog;
    this.properties = properties;
  }

  public List<StudioFrame> getFrames() {
    return catalog.frames().stream()
        .map(
            frame ->
                new StudioFrame(
                    frame.frameType(),
                    frame.displayName(),
                    properties.overlayImageUrl(frame.frameType())))
        .filter(frame -> !frame.overlayImageUrl().isBlank())
        .toList();
  }

  public record StudioFrame(FrameType frameType, String displayName, String overlayImageUrl) {}
}
