// 직원의 현재 방문 고객 목록 조회 API를 처리하는 컨트롤러
package com.lionthanflower.controller.staff;

import com.lionthanflower.application.staff.StaffVisitService;
import com.lionthanflower.application.staff.dto.StaffVisitListResponse;
import com.lionthanflower.application.staff.dto.VisitSummaryResponse;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.store.error.StaffErrorCode;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.response.ApiResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaffVisitController {

  private final StaffVisitService staffVisitService;

  public StaffVisitController(StaffVisitService staffVisitService) {
    this.staffVisitService = staffVisitService;
  }

  @GetMapping("/api/staff/visits")
  public ApiResponse<StaffVisitListResponse> getCurrentVisits(
      @AuthenticationPrincipal Staff staff) {
    if (staff == null) {
      throw new BusinessException(StaffErrorCode.UNAUTHORIZED);
    }
    List<VisitSummaryResponse> visits = staffVisitService.getCurrentVisits(staff.getStoreId());
    return ApiResponse.success(new StaffVisitListResponse(visits));
  }
}
