// MCM Studio 프레임 URL 설정의 바인딩과 검증을 확인하는 테스트
package com.lionthanflower.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.lionthanflower.domain.myself.entity.FrameType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class StudioFramePropertiesTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(McmStudioConfig.class);

  @Test
  void URL이_비어_있으면_프레임을_설정하지_않는다() {
    contextRunner
        .withPropertyValues(
            "app.mcm-studio.frame-overlay-urls.FRAME_1=",
            "app.mcm-studio.frame-overlay-urls.FRAME_2=")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              StudioFrameProperties properties = context.getBean(StudioFrameProperties.class);
              assertThat(properties.overlayImageUrl(FrameType.FRAME_1)).isEmpty();
              assertThat(properties.overlayImageUrl(FrameType.FRAME_2)).isEmpty();
            });
  }

  @Test
  void HTTP와_HTTPS_절대_URL을_허용하고_공백을_제거한다() {
    contextRunner
        .withPropertyValues(
            "app.mcm-studio.frame-overlay-urls.FRAME_1= https://cdn.example.com/frame-1.png ",
            "app.mcm-studio.frame-overlay-urls.FRAME_2=http://cdn.example.com/frame-2.png")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              StudioFrameProperties properties = context.getBean(StudioFrameProperties.class);
              assertThat(properties.overlayImageUrl(FrameType.FRAME_1))
                  .isEqualTo("https://cdn.example.com/frame-1.png");
              assertThat(properties.overlayImageUrl(FrameType.FRAME_2))
                  .isEqualTo("http://cdn.example.com/frame-2.png");
            });
  }

  @Test
  void 상대_경로는_애플리케이션_시작_단계에서_거부한다() {
    contextRunner
        .withPropertyValues("app.mcm-studio.frame-overlay-urls.FRAME_1=/frame-1.png")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasRootCauseMessage("MCM Studio 프레임 URL은 HTTP 또는 HTTPS 절대 URL이어야 합니다: FRAME_1");
            });
  }

  @Test
  void HTTP가_아닌_URL은_애플리케이션_시작_단계에서_거부한다() {
    contextRunner
        .withPropertyValues("app.mcm-studio.frame-overlay-urls.FRAME_2=file:///frame-2.png")
        .run(
            context -> {
              assertThat(context).hasFailed();
              assertThat(context.getStartupFailure())
                  .hasRootCauseMessage("MCM Studio 프레임 URL은 HTTP 또는 HTTPS 절대 URL이어야 합니다: FRAME_2");
            });
  }
}
