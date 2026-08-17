// 직원 Arc 생성 요청과 OpenAI 결과 저장 오케스트레이션을 검증하는 테스트
package com.lionthanflower.application.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lionthanflower.application.arc.ArcGenerationCommand;
import com.lionthanflower.application.arc.ArcGenerationPort;
import com.lionthanflower.application.staff.dto.StaffArcGenerationRequest;
import com.lionthanflower.application.staff.dto.StaffArcRevisionResponse;
import com.lionthanflower.domain.arc.entity.ActualInteractionPreference;
import com.lionthanflower.domain.arc.entity.ArcGeneratedContent;
import com.lionthanflower.domain.arc.entity.ArcInputSnapshot;
import com.lionthanflower.domain.arc.entity.ArcRevisionStatus;
import com.lionthanflower.domain.arc.entity.PreferredColor;
import com.lionthanflower.domain.arc.entity.PreferredStyle;
import com.lionthanflower.domain.arc.entity.ProductExplanationPreference;
import com.lionthanflower.domain.arc.entity.PurchaseCriterion;
import com.lionthanflower.domain.arc.entity.PurchaseDecisionStyle;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.product.entity.ProductCategory;
import com.lionthanflower.domain.store.entity.Staff;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffArcServiceTest {

  @Mock private StaffArcStateService stateService;
  @Mock private ArcGenerationPort generationPort;

  private StaffArcService service;

  @BeforeEach
  void setUp() {
    service = new StaffArcService(stateService, generationPort);
  }

  @Test
  void Arc_생성_성공은_OpenAI_결과를_READY로_저장한다() {
    UUID visitId = UUID.randomUUID();
    UUID arcId = UUID.randomUUID();
    UUID revisionId = UUID.randomUUID();
    Staff staff = Staff.create(UUID.randomUUID(), "김형진", "hashed-token", Set.of(LanguageCode.EN));
    StaffArcGenerationRequest request = new StaffArcGenerationRequest(snapshot());
    ArcGenerationCommand command = new ArcGenerationCommand("홍길동", "컬러 요청", snapshot());
    StaffArcStateService.GenerationContext context =
        new StaffArcStateService.GenerationContext(arcId, revisionId, command);
    ArcGeneratedContent content = new ArcGeneratedContent("오늘의 순간", List.of("실용성"), "기억할 순간");
    StaffArcRevisionResponse expected =
        new StaffArcRevisionResponse(
            arcId,
            revisionId,
            1,
            com.lionthanflower.domain.arc.entity.ArcStatus.DRAFT,
            ArcRevisionStatus.READY,
            snapshot(),
            content,
            null,
            null);
    when(stateService.prepareInitial(visitId, staff, request)).thenReturn(context);
    when(generationPort.generate(command)).thenReturn(content);
    when(stateService.complete(revisionId, content)).thenReturn(expected);

    StaffArcRevisionResponse result = service.createArc(visitId, staff, request);

    assertThat(result).isSameAs(expected);
    verify(stateService).complete(revisionId, content);
  }

  @Test
  void OpenAI_실패는_FAILED_리비전을_저장한다() {
    UUID visitId = UUID.randomUUID();
    UUID revisionId = UUID.randomUUID();
    Staff staff = Staff.create(UUID.randomUUID(), "김형진", "hashed-token", Set.of(LanguageCode.EN));
    StaffArcGenerationRequest request = new StaffArcGenerationRequest(snapshot());
    ArcGenerationCommand command = new ArcGenerationCommand("홍길동", "컬러 요청", snapshot());
    StaffArcStateService.GenerationContext context =
        new StaffArcStateService.GenerationContext(UUID.randomUUID(), revisionId, command);
    StaffArcRevisionResponse expected =
        new StaffArcRevisionResponse(
            context.arcId(),
            revisionId,
            1,
            com.lionthanflower.domain.arc.entity.ArcStatus.DRAFT,
            ArcRevisionStatus.FAILED,
            snapshot(),
            null,
            "OPENAI_GENERATION_FAILED",
            null);
    when(stateService.prepareInitial(visitId, staff, request)).thenReturn(context);
    when(generationPort.generate(command))
        .thenThrow(new IllegalStateException("OpenAI unavailable"));
    when(stateService.fail(revisionId, "OPENAI_GENERATION_FAILED")).thenReturn(expected);

    StaffArcRevisionResponse result = service.createArc(visitId, staff, request);

    assertThat(result.revisionStatus()).isEqualTo(ArcRevisionStatus.FAILED);
    verify(stateService).fail(revisionId, "OPENAI_GENERATION_FAILED");
  }

  private ArcInputSnapshot snapshot() {
    return new ArcInputSnapshot(
        java.time.LocalDate.of(2026, 8, 13),
        "KOREA",
        "MCM HAUS",
        List.of(UUID.randomUUID()),
        Set.of(ProductCategory.BAG),
        Set.of(PreferredColor.BLACK),
        null,
        Set.of(PreferredStyle.MINIMAL_SIMPLE),
        null,
        List.of(),
        Set.of(PurchaseCriterion.DESIGN),
        null,
        Set.of(ActualInteractionPreference.ACTIVE_RECOMMENDATION),
        Set.of(ProductExplanationPreference.KEY_POINTS_ONLY),
        PurchaseDecisionStyle.QUICK,
        "차분한 응대를 선호함");
  }
}
