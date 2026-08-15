// Arc 리비전 생성과 공유 및 고객 최종 저장을 검증하는 테스트
package com.lionthanflower.domain.arc.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArcTest {

  @Test
  void 직원이_READY_리비전을_고객에게_공유한다() {
    UUID staffId = UUID.randomUUID();
    Arc arc = Arc.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), staffId);
    ArcRevision revision =
        ArcRevision.start(arc.getId(), 1, "{\"schemaVersion\":1}", "arc-v1", staffId);
    revision.complete("{\"title\":\"My MCM\"}", Instant.parse("2026-08-15T12:00:00Z"));

    arc.share(revision, Instant.parse("2026-08-15T12:01:00Z"));

    assertThat(arc.getStatus()).isEqualTo(ArcStatus.SHARED);
    assertThat(arc.getSharedRevisionId()).isEqualTo(revision.getId());
    assertThat(revision.getSharedAt()).isNotNull();
  }

  @Test
  void 고객이_현재_공유된_리비전을_최종_저장한다() {
    ArcAndRevision fixture = sharedArc();
    Instant finalizedAt = Instant.parse("2026-08-15T12:05:00Z");

    fixture.arc().finalizeSharedRevision(finalizedAt);

    assertThat(fixture.arc().getStatus()).isEqualTo(ArcStatus.FINALIZED);
    assertThat(fixture.arc().getFinalRevisionId()).isEqualTo(fixture.revision().getId());
    assertThat(fixture.arc().getFinalizedAt()).isEqualTo(finalizedAt);
  }

  @Test
  void 생성_실패한_리비전은_공유할_수_없다() {
    Arc arc =
        Arc.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    ArcRevision revision =
        ArcRevision.start(
            arc.getId(), 1, "{\"schemaVersion\":1}", "arc-v1", arc.getCreatedByStaffId());
    revision.fail("OPENAI_UNAVAILABLE");

    assertThatThrownBy(() -> arc.share(revision, Instant.now()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("생성이 완료된 Arc 리비전만 공유할 수 있습니다.");
  }

  @Test
  void 최종_저장된_Arc에는_새_리비전을_공유할_수_없다() {
    ArcAndRevision fixture = sharedArc();
    fixture.arc().finalizeSharedRevision(Instant.now());
    ArcRevision next =
        ArcRevision.start(
            fixture.arc().getId(),
            2,
            "{\"schemaVersion\":1}",
            "arc-v1",
            fixture.arc().getCreatedByStaffId());
    next.complete("{\"title\":\"Next\"}", Instant.now());

    assertThatThrownBy(() -> fixture.arc().share(next, Instant.now()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("최종 저장된 Arc는 다시 공유할 수 없습니다.");
  }

  @Test
  void 생성_완료_시각이_없으면_리비전_결과를_변경하지_않는다() {
    ArcRevision revision =
        ArcRevision.start(
            UUID.randomUUID(), 1, "{\"schemaVersion\":1}", "arc-v1", UUID.randomUUID());

    assertThatThrownBy(() -> revision.complete("{\"title\":\"My MCM\"}", null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Arc 생성 완료 시각은 null일 수 없습니다.");
    assertThat(revision.getStatus()).isEqualTo(ArcRevisionStatus.GENERATING);
    assertThat(revision.getGeneratedContent()).isNull();
    assertThat(revision.getGeneratedAt()).isNull();
  }

  @Test
  void 다른_Arc의_리비전은_공유할_수_없다() {
    Arc arc =
        Arc.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    ArcRevision otherRevision =
        ArcRevision.start(
            UUID.randomUUID(), 1, "{\"schemaVersion\":1}", "arc-v1", UUID.randomUUID());
    otherRevision.complete("{\"title\":\"Other\"}", Instant.now());

    assertThatThrownBy(() -> arc.share(otherRevision, Instant.now()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("같은 Arc의 리비전만 공유할 수 있습니다.");
  }

  @Test
  void 공유되지_않은_Arc는_최종_저장할_수_없다() {
    Arc arc =
        Arc.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

    assertThatThrownBy(() -> arc.finalizeSharedRevision(Instant.now()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("고객에게 공유된 Arc만 최종 저장할 수 있습니다.");
  }

  private ArcAndRevision sharedArc() {
    UUID staffId = UUID.randomUUID();
    Arc arc = Arc.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), staffId);
    ArcRevision revision =
        ArcRevision.start(arc.getId(), 1, "{\"schemaVersion\":1}", "arc-v1", staffId);
    revision.complete("{\"title\":\"My MCM\"}", Instant.parse("2026-08-15T12:00:00Z"));
    arc.share(revision, Instant.parse("2026-08-15T12:01:00Z"));
    return new ArcAndRevision(arc, revision);
  }

  private record ArcAndRevision(Arc arc, ArcRevision revision) {}
}
