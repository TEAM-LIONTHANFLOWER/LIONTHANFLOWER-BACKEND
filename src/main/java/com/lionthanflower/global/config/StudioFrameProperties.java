// MCM Studio 프레임별 합성 이미지 URL 설정을 바인딩하고 검증하는 객체
package com.lionthanflower.global.config;

import com.lionthanflower.domain.myself.entity.FrameType;
import java.net.URI;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mcm-studio")
public record StudioFrameProperties(Map<FrameType, String> frameOverlayUrls) {

  public StudioFrameProperties {
    EnumMap<FrameType, String> normalized = new EnumMap<>(FrameType.class);
    if (frameOverlayUrls != null) {
      frameOverlayUrls.forEach(
          (frameType, value) -> normalized.put(frameType, normalize(frameType, value)));
    }
    frameOverlayUrls = Collections.unmodifiableMap(normalized);
  }

  public String overlayImageUrl(FrameType frameType) {
    return frameOverlayUrls.getOrDefault(frameType, "");
  }

  private static String normalize(FrameType frameType, String value) {
    if (value == null || value.isBlank()) {
      return "";
    }

    String normalized = value.trim();
    URI uri;
    try {
      uri = URI.create(normalized);
    } catch (IllegalArgumentException exception) {
      throw invalidUrl(frameType, exception);
    }

    boolean supportedScheme =
        "http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme());
    if (!supportedScheme || uri.getHost() == null) {
      throw invalidUrl(frameType, null);
    }
    return normalized;
  }

  private static IllegalArgumentException invalidUrl(
      FrameType frameType, IllegalArgumentException cause) {
    String message = "MCM Studio 프레임 URL은 HTTP 또는 HTTPS 절대 URL이어야 합니다: " + frameType;
    return cause == null
        ? new IllegalArgumentException(message)
        : new IllegalArgumentException(message, cause);
  }
}
