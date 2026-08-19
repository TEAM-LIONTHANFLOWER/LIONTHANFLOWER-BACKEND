// 직원 Arc 생성·조회·재생성·공유 API를 처리하는 컨트롤러
package com.lionthanflower.controller.staff;

import com.lionthanflower.application.staff.StaffArcService;
import com.lionthanflower.application.staff.dto.StaffArcGenerationRequest;
import com.lionthanflower.application.staff.dto.StaffArcRevisionResponse;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.store.error.StaffErrorCode;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaffArcController {

  private final StaffArcService staffArcService;

  public StaffArcController(StaffArcService staffArcService) {
    this.staffArcService = staffArcService;
  }

  @Operation(summary = "Arc 생성", description = "직원 입력을 바탕으로 OpenAI Arc 생성을 시작합니다.")
  @PostMapping("/api/staff/visits/{visitId}/arcs")
  public ApiResponse<StaffArcRevisionResponse> createArc(
      @PathVariable UUID visitId,
      @RequestBody StaffArcGenerationRequest request,
      @AuthenticationPrincipal Staff staff) {
    return ApiResponse.success(staffArcService.createArc(visitId, requireStaff(staff), request));
  }

  @Operation(summary = "직원 Arc 미리보기", description = "직원이 생성 중이거나 생성된 Arc 리비전을 확인합니다.")
  @GetMapping("/api/staff/arcs/{arcId}")
  public ApiResponse<StaffArcRevisionResponse> getPreview(
      @PathVariable UUID arcId, @AuthenticationPrincipal Staff staff) {
    return ApiResponse.success(staffArcService.getPreview(arcId, requireStaff(staff)));
  }

  @Operation(summary = "Arc 재생성", description = "기존 또는 수정된 입력으로 새로운 Arc 리비전을 생성합니다.")
  @PostMapping("/api/staff/arcs/{arcId}/revisions")
  public ApiResponse<StaffArcRevisionResponse> regenerate(
      @PathVariable UUID arcId,
      @RequestBody StaffArcGenerationRequest request,
      @AuthenticationPrincipal Staff staff) {
    return ApiResponse.success(staffArcService.regenerate(arcId, requireStaff(staff), request));
  }

  private Staff requireStaff(Staff staff) {
    if (staff == null) {
      throw new BusinessException(StaffErrorCode.UNAUTHORIZED);
    }
    return staff;
  }
}
