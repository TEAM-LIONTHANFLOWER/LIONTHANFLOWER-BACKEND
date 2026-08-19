// 직원 등록 화면에 공개 매장 검색 HTTP API를 제공하는 Controller
package com.lionthanflower.infrastructure.web.store;

import com.lionthanflower.application.store.StoreQueryService;
import com.lionthanflower.application.store.StoreQueryService.StoreSummary;
import com.lionthanflower.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StoreController {

  private final StoreQueryService storeQueryService;

  public StoreController(StoreQueryService storeQueryService) {
    this.storeQueryService = storeQueryService;
  }

  @Operation(summary = "매장 검색", description = "직원 프로필 등록 전에 이름 또는 코드로 매장을 검색합니다.")
  @GetMapping("/api/stores")
  public ApiResponse<List<StoreSummary>> search(@RequestParam(defaultValue = "") String query) {
    return ApiResponse.success(storeQueryService.search(query));
  }
}
