// 직원 Visit Memory 생성·미리보기·재생성·공유 API를 검증하는 테스트
package com.lionthanflower.controller.staff;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionthanflower.application.staff.StaffVisitMemoryService;
import com.lionthanflower.application.staff.dto.StaffVisitMemoryResponse;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryGeneratedContent;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryInputSnapshot;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryStatus;
import com.lionthanflower.global.config.CustomerApiSecurityConfig;
import com.lionthanflower.global.error.GlobalExceptionHandler;
import java.util.Map;
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

@WebMvcTest(StaffVisitMemoryController.class)
@AutoConfigureMockMvc
@Import({GlobalExceptionHandler.class, CustomerApiSecurityConfig.class})
class StaffVisitMemoryControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private StaffVisitMemoryService staffVisitMemoryService;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void Visit_Memory_생성_API는_READY_미리보기를_반환한다() throws Exception {
    UUID visitId = UUID.randomUUID();
    Staff staff = staff();
    StaffVisitMemoryResponse response = response(UUID.randomUUID(), VisitMemoryStatus.READY);
    when(staffVisitMemoryService.create(eq(visitId), eq(staff), any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/staff/visits/{visitId}/visit-memories", visitId)
                .contentType("application/json")
                .content("{}")
                .with(authentication(staffAuthentication(staff))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("READY"));

    verify(staffVisitMemoryService).create(eq(visitId), eq(staff), any());
  }

  @Test
  void Visit_Memory_미리보기와_재생성과_공유_API를_호출한다() throws Exception {
    UUID memoryId = UUID.randomUUID();
    UUID visitId = UUID.randomUUID();
    UUID revisionResponseVisitId = visitId;
    Staff staff = staff();
    StaffVisitMemoryResponse response = response(memoryId, VisitMemoryStatus.READY);
    when(staffVisitMemoryService.getPreview(memoryId, staff)).thenReturn(response);
    when(staffVisitMemoryService.regenerate(eq(memoryId), eq(staff), any())).thenReturn(response);
    when(staffVisitMemoryService.share(memoryId, staff)).thenReturn(response);

    mockMvc
        .perform(
            get("/api/staff/visit-memories/{visitMemoryId}", memoryId)
                .with(authentication(staffAuthentication(staff))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.visitMemoryId").value(memoryId.toString()));
    mockMvc
        .perform(
            post("/api/staff/visit-memories/{visitMemoryId}/regenerations", memoryId)
                .contentType("application/json")
                .content("{}")
                .with(authentication(staffAuthentication(staff))))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/api/staff/visit-memories/{visitMemoryId}/share", memoryId)
                .with(authentication(staffAuthentication(staff))))
        .andExpect(status().isOk());

    verify(staffVisitMemoryService).getPreview(memoryId, staff);
    verify(staffVisitMemoryService).regenerate(eq(memoryId), eq(staff), any());
    verify(staffVisitMemoryService).share(memoryId, staff);
  }

  @Test
  void 미인증_Visit_Memory_생성_요청은_401을_반환한다() throws Exception {
    mockMvc
        .perform(
            post("/api/staff/visits/{visitId}/visit-memories", UUID.randomUUID())
                .contentType("application/json")
                .content("{}")
                .with(anonymous()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error.code").value("STAFF-401"));

    verify(staffVisitMemoryService, never()).create(any(), any(), any());
  }

  private Staff staff() {
    return Staff.create(UUID.randomUUID(), "김형진", "hashed-token", Set.of(LanguageCode.EN));
  }

  private UsernamePasswordAuthenticationToken staffAuthentication(Staff staff) {
    return new UsernamePasswordAuthenticationToken(staff, null, java.util.List.of());
  }

  private StaffVisitMemoryResponse response(UUID memoryId, VisitMemoryStatus status) {
    VisitMemoryInputSnapshot snapshot =
        new VisitMemoryInputSnapshot(Map.of(), Set.of(), null, Set.of(), null, null);
    return new StaffVisitMemoryResponse(
        memoryId,
        UUID.randomUUID(),
        status,
        snapshot,
        new VisitMemoryGeneratedContent("방문 기록"),
        null,
        null,
        null);
  }
}
