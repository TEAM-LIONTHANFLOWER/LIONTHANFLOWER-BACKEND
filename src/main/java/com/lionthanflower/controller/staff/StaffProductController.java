// 직원 제품 목록 조회 API를 처리하는 컨트롤러
package com.lionthanflower.controller.staff;

import com.lionthanflower.application.staff.StaffProductService;
import com.lionthanflower.application.staff.dto.StaffProductResponse;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.store.error.StaffErrorCode;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StaffProductController {

  private final StaffProductService staffProductService;

  public StaffProductController(StaffProductService staffProductService) {
    this.staffProductService = staffProductService;
  }

  @Operation(
      summary = "직원 제품 목록 조회",
      description = "Arc와 Visit Memory 생성에 사용할 제품 및 제품 Variant 목록을 조회합니다.")
  @GetMapping("/api/staff/products")
  public ApiResponse<List<StaffProductResponse>> getProducts(@AuthenticationPrincipal Staff staff) {
    if (staff == null) {
      throw new BusinessException(StaffErrorCode.UNAUTHORIZED);
    }
    return ApiResponse.success(staffProductService.getProducts());
  }
}
