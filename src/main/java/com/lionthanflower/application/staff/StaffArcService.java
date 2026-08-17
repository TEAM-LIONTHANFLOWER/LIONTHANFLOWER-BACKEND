// 직원 Arc 생성 요청과 OpenAI 결과 저장을 조정하는 Application Service
package com.lionthanflower.application.staff;

import com.lionthanflower.application.arc.ArcGenerationPort;
import com.lionthanflower.application.staff.dto.StaffArcGenerationRequest;
import com.lionthanflower.application.staff.dto.StaffArcRevisionResponse;
import com.lionthanflower.domain.arc.entity.ArcGeneratedContent;
import com.lionthanflower.domain.store.entity.Staff;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class StaffArcService {

  private static final String OPENAI_FAILURE_CODE = "OPENAI_GENERATION_FAILED";

  private final StaffArcStateService stateService;
  private final ArcGenerationPort generationPort;

  public StaffArcService(StaffArcStateService stateService, ArcGenerationPort generationPort) {
    this.stateService = stateService;
    this.generationPort = generationPort;
  }

  public StaffArcRevisionResponse createArc(
      UUID visitId, Staff staff, StaffArcGenerationRequest request) {
    StaffArcStateService.GenerationContext context =
        stateService.prepareInitial(visitId, staff, request);
    return generate(context);
  }

  public StaffArcRevisionResponse regenerate(
      UUID arcId, Staff staff, StaffArcGenerationRequest request) {
    StaffArcStateService.GenerationContext context =
        stateService.prepareRevision(arcId, staff, request);
    return generate(context);
  }

  public StaffArcRevisionResponse getPreview(UUID arcId, Staff staff) {
    return stateService.getPreview(arcId, staff);
  }

  public StaffArcRevisionResponse share(UUID arcId, UUID revisionId, Staff staff) {
    return stateService.share(arcId, revisionId, staff);
  }

  private StaffArcRevisionResponse generate(StaffArcStateService.GenerationContext context) {
    ArcGeneratedContent generatedContent;
    try {
      generatedContent = generationPort.generate(context.generationCommand());
    } catch (RuntimeException exception) {
      return stateService.fail(context.revisionId(), OPENAI_FAILURE_CODE);
    }
    return stateService.complete(context.revisionId(), generatedContent);
  }
}
