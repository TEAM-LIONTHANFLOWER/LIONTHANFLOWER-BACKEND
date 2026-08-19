// 고객 Arc 목록과 상세 조회 HTTP 응답을 검증하는 Controller 테스트
package com.lionthanflower.infrastructure.web.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionthanflower.application.customer.CustomerArcQueryService;
import com.lionthanflower.domain.arc.entity.ArcStatus;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.error.CommonErrorCode;
import com.lionthanflower.global.error.GlobalExceptionHandler;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.Cookie;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerArcController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CustomerArcControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CustomerArcQueryService service;

  @Test
  void 고객_Arc_목록을_대표_제품과_함께_반환한다() throws Exception {
    UUID arcId = UUID.randomUUID();
    CustomerArcQueryService.ProductView product =
        new CustomerArcQueryService.ProductView(
            UUID.randomUUID(),
            "A Bag",
            com.lionthanflower.domain.product.entity.ProductColor.BLACK,
            com.lionthanflower.domain.product.entity.ProductOption.S);
    when(service.getArcs("known-token"))
        .thenReturn(
            List.of(
                new CustomerArcQueryService.ArcSummary(
                    arcId,
                    2,
                    "MCM HAUS",
                    "균형을 중요하게 생각합니다.",
                    "수납공간을 오래 고민했습니다.",
                    product,
                    ArcStatus.SHARED,
                    Instant.parse("2026-08-15T12:01:00Z"),
                    null)));

    mockMvc
        .perform(get("/api/customers/arcs").cookie(new Cookie("customer_token", "known-token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].arcId").value(arcId.toString()))
        .andExpect(jsonPath("$.data[0].arcNumber").value(2))
        .andExpect(jsonPath("$.data[0].storeName").value("MCM HAUS"))
        .andExpect(jsonPath("$.data[0].momentSummary").value("균형을 중요하게 생각합니다."))
        .andExpect(jsonPath("$.data[0].momentToRemember").value("수납공간을 오래 고민했습니다."))
        .andExpect(jsonPath("$.data[0].representativeProduct.productName").value("A Bag"))
        .andExpect(jsonPath("$.data[0].representativeProduct.imageObjectKey").doesNotExist())
        .andExpect(jsonPath("$.data[0].status").value("SHARED"));
  }

  @Test
  void 고객_Arc_목록_API에_매장과_편지_본문_설명이_있다() throws NoSuchMethodException {
    Method method = CustomerArcController.class.getMethod("getArcs", String.class);

    Operation operation = method.getAnnotation(Operation.class);

    assertThat(operation).isNotNull();
    assertThat(operation.description()).contains("매장 이름", "편지 본문");
  }

  @Test
  void 고객_Arc_상세와_전체_구매_제품을_반환한다() throws Exception {
    UUID arcId = UUID.randomUUID();
    CustomerArcQueryService.ProductView product =
        new CustomerArcQueryService.ProductView(
            UUID.randomUUID(),
            "A Bag",
            com.lionthanflower.domain.product.entity.ProductColor.BLACK,
            com.lionthanflower.domain.product.entity.ProductOption.S);
    when(service.getArc(arcId, "known-token"))
        .thenReturn(
            new CustomerArcQueryService.ArcDetail(
                arcId,
                2,
                "Ethan",
                "MCM HAUS",
                "KR",
                ArcStatus.FINALIZED,
                Instant.parse("2026-08-15T12:01:00Z"),
                Instant.parse("2026-08-15T12:05:00Z"),
                "균형을 중요하게 생각합니다.",
                List.of("실용적인 디자인", "차분한 컬러"),
                "수납공간을 오래 고민했습니다.",
                List.of(product)));

    mockMvc
        .perform(
            get("/api/customers/arcs/{arcId}", arcId)
                .cookie(new Cookie("customer_token", "known-token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.customerName").value("Ethan"))
        .andExpect(jsonPath("$.data.storeName").value("MCM HAUS"))
        .andExpect(jsonPath("$.data.preferences[1]").value("차분한 컬러"))
        .andExpect(jsonPath("$.data.purchasedProducts.length()").value(1))
        .andExpect(jsonPath("$.data.purchasedProducts[0].imageObjectKey").doesNotExist());
  }

  @Test
  void 고객_토큰이_없으면_401을_반환한다() throws Exception {
    when(service.getArcs(null)).thenThrow(new BusinessException(CommonErrorCode.UNAUTHORIZED));

    mockMvc
        .perform(get("/api/customers/arcs"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("COMMON-401"));
  }

}
