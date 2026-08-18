// 직원 Visit Memory 생성 요청과 OpenAI 결과 저장을 조정하는 Application Service
package com.lionthanflower.application.staff;

import com.lionthanflower.application.staff.dto.StaffVisitMemoryGenerationRequest;
import com.lionthanflower.application.staff.dto.StaffVisitMemoryResponse;
import com.lionthanflower.application.visitmemory.VisitMemoryGenerationPort;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryGeneratedContent;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class StaffVisitMemoryService {

  private static final String OPENAI_FAILURE_CODE = "OPENAI_GENERATION_FAILED";

  private final StaffVisitMemoryStateService stateService;
  private final VisitMemoryGenerationPort generationPort;

  public StaffVisitMemoryService(
      StaffVisitMemoryStateService stateService, VisitMemoryGenerationPort generationPort) {
    this.stateService = stateService;
    this.generationPort = generationPort;
  }

  public StaffVisitMemoryResponse create(
      UUID visitId, Staff staff, StaffVisitMemoryGenerationRequest request) {
    return generate(stateService.prepareInitial(visitId, staff, request));
  }

  public StaffVisitMemoryResponse regenerate(
      UUID visitMemoryId, Staff staff, StaffVisitMemoryGenerationRequest request) {
    return generate(stateService.prepareRegeneration(visitMemoryId, staff, request));
  }

  public StaffVisitMemoryResponse getPreview(UUID visitMemoryId, Staff staff) {
    return stateService.getPreview(visitMemoryId, staff);
  }

  public StaffVisitMemoryResponse share(UUID visitMemoryId, Staff staff) {
    return stateService.share(visitMemoryId, staff);
  }

  private StaffVisitMemoryResponse generate(
      StaffVisitMemoryStateService.GenerationContext context) {
    VisitMemoryGeneratedContent generatedContent;
    try {
      generatedContent = generationPort.generate(context.generationCommand());
    } catch (RuntimeException exception) {
      return stateService.fail(context.visitMemoryId(), OPENAI_FAILURE_CODE);
    }
    return stateService.complete(context.visitMemoryId(), generatedContent);
  }
}
