// 고객용 MCM Studio 프레임 목록 HTTP 응답을 검증하는 테스트
package com.lionthanflower.infrastructure.web.customer;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionthanflower.application.studio.StudioFrameService;
import com.lionthanflower.domain.myself.entity.FrameType;
import com.lionthanflower.global.config.CustomerApiSecurityConfig;
import com.lionthanflower.global.error.GlobalExceptionHandler;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerStudioFrameController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, CustomerApiSecurityConfig.class})
class CustomerStudioFrameControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private StudioFrameService service;

  @Test
  void 고객_세션_없이_프레임_목록을_순서대로_조회한다() throws Exception {
    when(service.getFrames())
        .thenReturn(
            List.of(
                new StudioFrameService.StudioFrame(
                    FrameType.FRAME_1, "MCM Frame 1", "/mcm-studio/Frame_1.png"),
                new StudioFrameService.StudioFrame(
                    FrameType.FRAME_2, "MCM Frame 2", "/mcm-studio/Frame_2.png"),
                new StudioFrameService.StudioFrame(
                    FrameType.FRAME_3, "MCM Frame 3", "/mcm-studio/Frame_3.png"),
                new StudioFrameService.StudioFrame(
                    FrameType.FRAME_4, "MCM Frame 4", "/mcm-studio/Frame_4.png")));

    mockMvc
        .perform(get("/api/customers/studio/frames"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.length()").value(4))
        .andExpect(jsonPath("$.data[0].frameType").value("FRAME_1"))
        .andExpect(jsonPath("$.data[0].displayName").value("MCM Frame 1"))
        .andExpect(jsonPath("$.data[0].overlayImageUrl").value("/mcm-studio/Frame_1.png"))
        .andExpect(jsonPath("$.data[1].frameType").value("FRAME_2"))
        .andExpect(jsonPath("$.data[1].displayName").value("MCM Frame 2"))
        .andExpect(jsonPath("$.data[1].overlayImageUrl").value("/mcm-studio/Frame_2.png"))
        .andExpect(jsonPath("$.data[2].frameType").value("FRAME_3"))
        .andExpect(jsonPath("$.data[2].displayName").value("MCM Frame 3"))
        .andExpect(jsonPath("$.data[2].overlayImageUrl").value("/mcm-studio/Frame_3.png"))
        .andExpect(jsonPath("$.data[3].frameType").value("FRAME_4"))
        .andExpect(jsonPath("$.data[3].displayName").value("MCM Frame 4"))
        .andExpect(jsonPath("$.data[3].overlayImageUrl").value("/mcm-studio/Frame_4.png"));
  }
}
