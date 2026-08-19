// 직원의 현재 방문 고객 조회와 응대 시작 API를 처리하는 컨트롤러
package com.lionthanflower.controller.staff;

import com.lionthanflower.application.staff.StaffVisitService;
import com.lionthanflower.application.staff.dto.StaffVisitAssignmentResponse;
import com.lionthanflower.application.staff.dto.StaffVisitListResponse;
import com.lionthanflower.application.staff.dto.VisitSummaryResponse;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.store.error.StaffErrorCode;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaffVisitController {

  private final StaffVisitService staffVisitService;

  public StaffVisitController(StaffVisitService staffVisitService) {
    this.staffVisitService = staffVisitService;
  }

  @Operation(
      summary = "현재 방문 고객 목록 조회",
      description = "직원이 현재 방문 중인 고객 목록과 방문 시각 및 응대 시작 시각을 조회합니다.")
  @GetMapping("/api/staff/visits")
  public ApiResponse<StaffVisitListResponse> getCurrentVisits(
      @AuthenticationPrincipal Staff staff) {
    if (staff == null) {
      throw new BusinessException(StaffErrorCode.UNAUTHORIZED);
    }
    List<VisitSummaryResponse> visits = staffVisitService.getCurrentVisits(staff.getStoreId());
    return ApiResponse.success(new StaffVisitListResponse(visits));
  }

  @Operation(
      summary = "직원 추천 고객 응대 시작",
      description = "대기 중인 직원 추천 고객에게 응대를 시작하고 현재 직원을 담당자로 배정합니다.")
  @PostMapping("/api/staff/visits/{visitId}/assignment")
  public ApiResponse<StaffVisitAssignmentResponse> assignVisit(
      @PathVariable UUID visitId, @AuthenticationPrincipal Staff staff) {
    if (staff == null) {
      throw new BusinessException(StaffErrorCode.UNAUTHORIZED);
    }
    return ApiResponse.success(staffVisitService.assignVisit(visitId, staff));
  }
}
