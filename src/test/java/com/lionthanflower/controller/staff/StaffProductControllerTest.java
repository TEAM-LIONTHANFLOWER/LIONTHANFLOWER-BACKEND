// 직원 제품 목록 조회 API의 인증과 응답 형식을 검증하는 테스트
package com.lionthanflower.controller.staff;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionthanflower.application.staff.StaffProductService;
import com.lionthanflower.application.staff.dto.StaffProductResponse;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.product.entity.ProductCategory;
import com.lionthanflower.domain.product.entity.ProductColor;
import com.lionthanflower.domain.product.entity.ProductOption;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.global.config.CustomerApiSecurityConfig;
import com.lionthanflower.global.error.GlobalExceptionHandler;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StaffProductController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, CustomerApiSecurityConfig.class})
class StaffProductControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private StaffProductService staffProductService;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void 인증된_직원에게_제품과_Variant_UUID를_반환한다() throws Exception {
    UUID productId = UUID.randomUUID();
    UUID variantId = UUID.randomUUID();
    Staff staff = Staff.create(UUID.randomUUID(), "김형진", "hashed-token", Set.of(LanguageCode.EN));
    when(staffProductService.getProducts())
        .thenReturn(
            List.of(
                new StaffProductResponse(
                    productId,
                    "MCM-BAG-001",
                    "Stark Backpack",
                    ProductCategory.BAG,
                    List.of(
                        new StaffProductResponse.VariantResponse(
                            variantId,
                            "MCM-BAG-001-BLACK-M",
                            ProductColor.BLACK,
                            ProductOption.M)))));

    mockMvc
        .perform(
            get("/api/staff/products")
                .with(
                    authentication(
                        new UsernamePasswordAuthenticationToken(staff, null, List.of()))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data[0].productId").value(productId.toString()))
        .andExpect(jsonPath("$.data[0].variants[0].productVariantId").value(variantId.toString()));
  }

  @Test
  void 인증되지_않은_제품_목록_요청은_401을_반환한다() throws Exception {
    mockMvc
        .perform(get("/api/staff/products").with(anonymous()))
        .andExpect(status().isUnauthorized());
  }
}
