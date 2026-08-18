// 직원 Visit Memory 생성·미리보기·재생성·공유 API를 처리하는 컨트롤러
package com.lionthanflower.controller.staff;

import com.lionthanflower.application.staff.StaffVisitMemoryService;
import com.lionthanflower.application.staff.dto.StaffVisitMemoryGenerationRequest;
import com.lionthanflower.application.staff.dto.StaffVisitMemoryResponse;
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
public class StaffVisitMemoryController {

  private final StaffVisitMemoryService staffVisitMemoryService;

  public StaffVisitMemoryController(StaffVisitMemoryService staffVisitMemoryService) {
    this.staffVisitMemoryService = staffVisitMemoryService;
  }

  @Operation(summary = "Visit Memory 생성", description = "직원 입력을 바탕으로 Visit Memory 생성을 시작합니다.")
  @PostMapping("/api/staff/visits/{visitId}/visit-memories")
  public ApiResponse<StaffVisitMemoryResponse> create(
      @PathVariable UUID visitId,
      @RequestBody StaffVisitMemoryGenerationRequest request,
      @AuthenticationPrincipal Staff staff) {
    return ApiResponse.success(
        staffVisitMemoryService.create(visitId, requireStaff(staff), request));
  }

  @Operation(
      summary = "직원 Visit Memory 미리보기",
      description = "직원이 현재 Visit Memory 입력과 생성 결과를 확인합니다.")
  @GetMapping("/api/staff/visit-memories/{visitMemoryId}")
  public ApiResponse<StaffVisitMemoryResponse> getPreview(
      @PathVariable UUID visitMemoryId, @AuthenticationPrincipal Staff staff) {
    return ApiResponse.success(
        staffVisitMemoryService.getPreview(visitMemoryId, requireStaff(staff)));
  }

  @Operation(summary = "Visit Memory 재생성", description = "기존 또는 수정된 입력으로 Visit Memory를 다시 생성합니다.")
  @PostMapping("/api/staff/visit-memories/{visitMemoryId}/regenerations")
  public ApiResponse<StaffVisitMemoryResponse> regenerate(
      @PathVariable UUID visitMemoryId,
      @RequestBody(required = false) StaffVisitMemoryGenerationRequest request,
      @AuthenticationPrincipal Staff staff) {
    return ApiResponse.success(
        staffVisitMemoryService.regenerate(visitMemoryId, requireStaff(staff), request));
  }

  @Operation(
      summary = "Visit Memory 전송",
      description = "직원이 READY Visit Memory를 고객에게 공유하고 알림을 생성합니다.")
  @PostMapping("/api/staff/visit-memories/{visitMemoryId}/share")
  public ApiResponse<StaffVisitMemoryResponse> share(
      @PathVariable UUID visitMemoryId, @AuthenticationPrincipal Staff staff) {
    return ApiResponse.success(staffVisitMemoryService.share(visitMemoryId, requireStaff(staff)));
  }

  private Staff requireStaff(Staff staff) {
    if (staff == null) {
      throw new BusinessException(StaffErrorCode.UNAUTHORIZED);
    }
    return staff;
  }
}
