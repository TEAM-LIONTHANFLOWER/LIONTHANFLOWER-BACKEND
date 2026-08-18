// 직원 Visit Memory 생성 요청과 OpenAI 결과 저장 오케스트레이션을 검증하는 테스트
package com.lionthanflower.application.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lionthanflower.application.staff.dto.StaffVisitMemoryGenerationRequest;
import com.lionthanflower.application.staff.dto.StaffVisitMemoryResponse;
import com.lionthanflower.application.visitmemory.VisitMemoryGenerationCommand;
import com.lionthanflower.application.visitmemory.VisitMemoryGenerationPort;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryGeneratedContent;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryInputSnapshot;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryStatus;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffVisitMemoryServiceTest {

  @Mock private StaffVisitMemoryStateService stateService;
  @Mock private VisitMemoryGenerationPort generationPort;

  private StaffVisitMemoryService service;

  @BeforeEach
  void setUp() {
    service = new StaffVisitMemoryService(stateService, generationPort);
  }

  @Test
  void Visit_Memory_생성_성공은_OpenAI_결과를_READY로_저장한다() {
    UUID visitId = UUID.randomUUID();
    UUID memoryId = UUID.randomUUID();
    Staff staff = staff();
    StaffVisitMemoryGenerationRequest request = new StaffVisitMemoryGenerationRequest(snapshot());
    VisitMemoryGenerationCommand command =
        new VisitMemoryGenerationCommand("홍길동", null, snapshot());
    StaffVisitMemoryStateService.GenerationContext context =
        new StaffVisitMemoryStateService.GenerationContext(memoryId, command);
    VisitMemoryGeneratedContent content = new VisitMemoryGeneratedContent("다음 방문을 준비한 기록");
    StaffVisitMemoryResponse expected = response(memoryId, VisitMemoryStatus.READY, content);
    when(stateService.prepareInitial(visitId, staff, request)).thenReturn(context);
    when(generationPort.generate(command)).thenReturn(content);
    when(stateService.complete(memoryId, content)).thenReturn(expected);

    StaffVisitMemoryResponse result = service.create(visitId, staff, request);

    assertThat(result).isSameAs(expected);
    verify(stateService).complete(memoryId, content);
  }

  @Test
  void OpenAI_실패는_FAILED_Visit_Memory를_저장한다() {
    UUID visitId = UUID.randomUUID();
    UUID memoryId = UUID.randomUUID();
    Staff staff = staff();
    StaffVisitMemoryGenerationRequest request = new StaffVisitMemoryGenerationRequest(snapshot());
    VisitMemoryGenerationCommand command =
        new VisitMemoryGenerationCommand("홍길동", null, snapshot());
    StaffVisitMemoryStateService.GenerationContext context =
        new StaffVisitMemoryStateService.GenerationContext(memoryId, command);
    StaffVisitMemoryResponse expected = response(memoryId, VisitMemoryStatus.FAILED, null);
    when(stateService.prepareInitial(visitId, staff, request)).thenReturn(context);
    when(generationPort.generate(command))
        .thenThrow(new IllegalStateException("OpenAI unavailable"));
    when(stateService.fail(memoryId, "OPENAI_GENERATION_FAILED")).thenReturn(expected);

    StaffVisitMemoryResponse result = service.create(visitId, staff, request);

    assertThat(result.status()).isEqualTo(VisitMemoryStatus.FAILED);
    verify(stateService).fail(memoryId, "OPENAI_GENERATION_FAILED");
  }

  private Staff staff() {
    return Staff.create(UUID.randomUUID(), "김형진", "hashed-token", Set.of(LanguageCode.EN));
  }

  private StaffVisitMemoryResponse response(
      UUID memoryId, VisitMemoryStatus status, VisitMemoryGeneratedContent content) {
    return new StaffVisitMemoryResponse(
        memoryId, UUID.randomUUID(), status, snapshot(), content, null, null, null);
  }

  private VisitMemoryInputSnapshot snapshot() {
    return new VisitMemoryInputSnapshot(Map.of(), Set.of(), null, Set.of(), null, null);
  }
}
