// 직원 등록 전에 사용할 공개 매장 검색 HTTP API를 검증하는 테스트
package com.lionthanflower.infrastructure.web.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionthanflower.application.store.StoreQueryService;
import com.lionthanflower.global.config.CustomerApiSecurityConfig;
import com.lionthanflower.global.error.GlobalExceptionHandler;
import io.swagger.v3.oas.annotations.Operation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StoreController.class)
@Import({GlobalExceptionHandler.class, CustomerApiSecurityConfig.class})
class StoreControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private StoreQueryService storeQueryService;

  @Test
  void 인증_없이_이름이나_코드로_매장을_검색한다() throws Exception {
    UUID storeId = UUID.randomUUID();
    when(storeQueryService.search("seoul"))
        .thenReturn(
            List.of(
                new StoreQueryService.StoreSummary(
                    storeId, "MCM Seoul", "MCM-SEOUL", "KR")));

    mockMvc
        .perform(get("/api/stores").queryParam("query", "seoul"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].storeId").value(storeId.toString()))
        .andExpect(jsonPath("$.data[0].name").value("MCM Seoul"))
        .andExpect(jsonPath("$.data[0].code").value("MCM-SEOUL"))
        .andExpect(jsonPath("$.data[0].countryCode").value("KR"));

    verify(storeQueryService).search("seoul");
  }

  @Test
  void 매장_검색_API에_OpenAPI_설명이_있다() throws NoSuchMethodException {
    Method method = StoreController.class.getMethod("search", String.class);

    Operation operation = method.getAnnotation(Operation.class);

    assertThat(operation).isNotNull();
    assertThat(operation.summary()).isEqualTo("매장 검색");
  }
}
