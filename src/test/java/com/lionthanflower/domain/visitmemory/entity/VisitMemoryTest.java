// Visit Memory 생성과 실패 재시도 상태 전이를 검증하는 테스트
package com.lionthanflower.domain.visitmemory.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VisitMemoryTest {

  @Test
  void 직원_저장으로_생성을_시작하고_최종_저장한다() {
    VisitMemory memory = createMemory();
    Instant generatedAt = Instant.parse("2026-08-15T13:00:00Z");

    memory.startGeneration();
    memory.complete("{\"summary\":\"MCM HAUS 방문\"}", generatedAt);

    assertThat(memory.getStatus()).isEqualTo(VisitMemoryStatus.FINALIZED);
    assertThat(memory.getGeneratedContent()).contains("MCM HAUS");
    assertThat(memory.getGeneratedAt()).isEqualTo(generatedAt);
    assertThat(memory.getFinalizedAt()).isEqualTo(generatedAt);
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
    memory.complete("{\"summary\":\"완료\"}", Instant.now());

    assertThatThrownBy(memory::startGeneration)
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("초안 또는 생성 실패 상태에서만 다시 생성할 수 있습니다.");
  }

  private VisitMemory createMemory() {
    return VisitMemory.create(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "{\"products\":[]}",
        "visit-memory-v1");
  }
}
