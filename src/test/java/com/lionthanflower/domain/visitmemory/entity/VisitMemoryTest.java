// Visit Memory 생성과 실패 재시도 상태 전이를 검증하는 테스트
package com.lionthanflower.domain.visitmemory.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VisitMemoryTest {

  @Test
  void 직원_저장으로_생성을_시작하고_미리보기_완료_후_전송한다() {
    VisitMemory memory = createMemory();
    Instant generatedAt = Instant.parse("2026-08-15T13:00:00Z");
    Instant finalizedAt = Instant.parse("2026-08-15T13:05:00Z");

    memory.startGeneration();
    memory.completeGeneration("{\"summary\":\"MCM HAUS 방문\"}", generatedAt);

    assertThat(memory.getStatus()).isEqualTo(VisitMemoryStatus.READY);
    assertThat(memory.getGeneratedContent()).contains("MCM HAUS");
    assertThat(memory.getGeneratedAt()).isEqualTo(generatedAt);
    assertThat(memory.getFinalizedAt()).isNull();

    memory.finalizeMemory(finalizedAt);

    assertThat(memory.getStatus()).isEqualTo(VisitMemoryStatus.FINALIZED);
    assertThat(memory.getFinalizedAt()).isEqualTo(finalizedAt);
  }

  @Test
  void 생성_실패_후_재시도할_수_있다() {
    VisitMemory memory = createMemory();
    memory.startGeneration();
    memory.fail("OPENAI_UNAVAILABLE");

    memory.startGeneration();

    assertThat(memory.getStatus()).isEqualTo(VisitMemoryStatus.GENERATING);
    assertThat(memory.getFailureCode()).isNull();
  }

  @Test
  void 최종_저장된_Visit_Memory는_다시_생성할_수_없다() {
    VisitMemory memory = createMemory();
    memory.startGeneration();
    memory.completeGeneration("{\"summary\":\"완료\"}", Instant.now());
    memory.finalizeMemory(Instant.now());

    assertThatThrownBy(memory::startGeneration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("초안, 생성 완료 또는 생성 실패 상태에서만 다시 생성할 수 있습니다.");
  }

  @Test
  void 생성_완료_시각이_없으면_Visit_Memory_결과를_변경하지_않는다() {
    VisitMemory memory = createMemory();
    memory.startGeneration();

    assertThatThrownBy(() -> memory.completeGeneration("{\"summary\":\"완료\"}", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Visit Memory 생성 완료 시각은 null일 수 없습니다.");
    assertThat(memory.getStatus()).isEqualTo(VisitMemoryStatus.GENERATING);
    assertThat(memory.getGeneratedContent()).isNull();
    assertThat(memory.getGeneratedAt()).isNull();
    assertThat(memory.getFinalizedAt()).isNull();
  }

  @Test
  void 생성_완료_또는_실패_상태에서_입력을_수정하고_재생성할_수_있다() {
    VisitMemory memory = createMemory();
    VisitMemoryInputSnapshot updatedInput =
        new VisitMemoryInputSnapshot(
            Map.of(),
            Set.of(CustomerInterestPoint.COLOR),
            null,
            Set.of(NoPurchaseReason.BUDGET),
            null,
            null);

    memory.startGeneration();
    memory.completeGeneration("{\"summary\":\"초안\"}", Instant.now());
    memory.replaceInput(updatedInput);
    memory.startGeneration();

    assertThat(memory.getStatus()).isEqualTo(VisitMemoryStatus.GENERATING);
    assertThat(memory.getInputSnapshot()).contains("COLOR");
  }

  @Test
  void 생성_실패_상태에서도_입력을_수정하고_재생성할_수_있다() {
    VisitMemory memory = createMemory();
    VisitMemoryInputSnapshot updatedInput =
        new VisitMemoryInputSnapshot(
            Map.of(),
            Set.of(CustomerInterestPoint.COLOR),
            null,
            Set.of(NoPurchaseReason.BUDGET),
            null,
            null);

    memory.startGeneration();
    memory.fail("OPENAI_UNAVAILABLE");
    memory.replaceInput(updatedInput);
    memory.startGeneration();

    assertThat(memory.getStatus()).isEqualTo(VisitMemoryStatus.GENERATING);
    assertThat(memory.getInputSnapshot()).contains("COLOR");
  }

  @Test
  void 검증된_Visit_Memory_입력을_JSON_스냅샷으로_저장한다() {
    VisitMemory memory = createMemory();

    assertThat(memory.getInputSnapshot()).contains("\"productEngagements\"");
    assertThat(memory.getInputSnapshot()).contains("VIEWED_WITH_INTEREST");
    assertThat(memory.getInputSnapshot()).contains("\"nextVisitMemo\":\"신상품 입고 시 안내\"");
  }

  @Test
  void 다음_방문_메모는_200자를_초과할_수_없다() {
    assertThatThrownBy(
            () ->
                new VisitMemoryInputSnapshot(
                    Map.of(), Set.of(), null, Set.of(), null, "가".repeat(201)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("다음 방문 메모는 200자를 초과할 수 없습니다.");
  }

  private VisitMemory createMemory() {
    return VisitMemory.create(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        new VisitMemoryInputSnapshot(
            Map.of(UUID.randomUUID(), Set.of(ProductEngagement.VIEWED_WITH_INTEREST)),
            Set.of(CustomerInterestPoint.DESIGN),
            null,
            Set.of(NoPurchaseReason.NEED_MORE_TIME),
            null,
            "신상품 입고 시 안내"),
        "visit-memory-v1");
  }
}
