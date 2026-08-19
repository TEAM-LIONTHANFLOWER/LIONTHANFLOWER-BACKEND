// 고객에게 MCM Studio 합성용 프레임 목록을 제공하는 Controller
package com.lionthanflower.infrastructure.web.customer;

import com.lionthanflower.application.studio.StudioFrameService;
import com.lionthanflower.domain.myself.entity.FrameType;
import com.lionthanflower.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customers/studio/frames")
public class CustomerStudioFrameController {

  private final StudioFrameService service;

  public CustomerStudioFrameController(StudioFrameService service) {
    this.service = service;
  }

  @Operation(
      summary = "MCM Studio 프레임 목록 조회",
      description = "웹 Canvas 합성에 사용할 프론트 정적 파일의 상대 경로를 포함한 SVG 프레임 목록을 반환합니다.")
  @GetMapping
  public ApiResponse<List<FrameResponse>> getFrames() {
    List<FrameResponse> frames = service.getFrames().stream().map(FrameResponse::from).toList();
    return ApiResponse.success(frames);
  }

  public record FrameResponse(FrameType frameType, String displayName, String overlayImageUrl) {

    static FrameResponse from(StudioFrameService.StudioFrame frame) {
      return new FrameResponse(frame.frameType(), frame.displayName(), frame.overlayImageUrl());
    }
  }
}
