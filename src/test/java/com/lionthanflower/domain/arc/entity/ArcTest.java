// Arc 리비전 생성과 공유 및 고객 최종 저장을 검증하는 테스트
package com.lionthanflower.domain.arc.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lionthanflower.domain.product.entity.ProductCategory;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArcTest {

  @Test
  void 직원이_READY_리비전을_고객에게_공유한다() {
    UUID staffId = UUID.randomUUID();
    Arc arc = Arc.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), staffId);
    ArcRevision revision = ArcRevision.start(arc.getId(), 1, snapshot(), "arc-v1", staffId);
    revision.complete(
        "{\"momentSummary\":\"My MCM\",\"preferences\":[\"디자인\"],\"momentToRemember\":\"첫 순간\"}",
        Instant.parse("2026-08-15T12:00:00Z"));

    arc.shareFirst(revision, Instant.parse("2026-08-15T12:01:00Z"), 1);

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
  void 최초_공유_순번은_재공유와_최종_저장에서도_유지된다() {
    UUID staffId = UUID.randomUUID();
    Arc arc = Arc.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), staffId);
    ArcRevision first = readyRevision(arc, 1, staffId);
    ArcRevision second = readyRevision(arc, 2, staffId);

    arc.shareFirst(first, Instant.parse("2026-08-15T12:01:00Z"), 2);
    arc.reshare(second, Instant.parse("2026-08-15T12:02:00Z"));
    arc.finalizeSharedRevision(Instant.parse("2026-08-15T12:03:00Z"));

    assertThat(arc.getArcNumber()).isEqualTo(2);
    assertThat(arc.getFinalRevisionId()).isEqualTo(second.getId());
  }

  @Test
  void Arc_순번은_1_이상이어야_한다() {
    Arc arc =
        Arc.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    ArcRevision revision = readyRevision(arc, 1, arc.getCreatedByStaffId());

    assertThatThrownBy(() -> arc.shareFirst(revision, Instant.now(), 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Arc 번호는 1 이상이어야 합니다.");
  }

  @Test
  void 생성_실패한_리비전은_공유할_수_없다() {
    Arc arc =
        Arc.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    ArcRevision revision =
        ArcRevision.start(arc.getId(), 1, snapshot(), "arc-v1", arc.getCreatedByStaffId());
    revision.fail("OPENAI_UNAVAILABLE");

    assertThatThrownBy(() -> arc.shareFirst(revision, Instant.now(), 1))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("생성이 완료된 Arc 리비전만 공유할 수 있습니다.");
  }

  @Test
  void 최종_저장된_Arc에는_새_리비전을_공유할_수_없다() {
    ArcAndRevision fixture = sharedArc();
    fixture.arc().finalizeSharedRevision(Instant.now());
    ArcRevision next =
        ArcRevision.start(
            fixture.arc().getId(), 2, snapshot(), "arc-v1", fixture.arc().getCreatedByStaffId());
    next.complete(
        "{\"momentSummary\":\"Next\",\"preferences\":[\"디자인\"],\"momentToRemember\":\"다음 순간\"}",
        Instant.now());

    assertThatThrownBy(() -> fixture.arc().reshare(next, Instant.now()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("최종 저장된 Arc는 다시 공유할 수 없습니다.");
  }

  @Test
  void 생성_완료_시각이_없으면_리비전_결과를_변경하지_않는다() {
    ArcRevision revision =
        ArcRevision.start(UUID.randomUUID(), 1, snapshot(), "arc-v1", UUID.randomUUID());

    assertThatThrownBy(
            () ->
                revision.complete(
                    "{\"momentSummary\":\"My MCM\",\"preferences\":[\"디자인\"],\"momentToRemember\":\"첫 순간\"}",
                    null))
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
        ArcRevision.start(UUID.randomUUID(), 1, snapshot(), "arc-v1", UUID.randomUUID());
    otherRevision.complete(
        "{\"momentSummary\":\"Other\",\"preferences\":[\"디자인\"],\"momentToRemember\":\"다른 순간\"}",
        Instant.now());

    assertThatThrownBy(() -> arc.shareFirst(otherRevision, Instant.now(), 1))
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

  @Test
  void 검증된_Arc_입력을_JSON_스냅샷으로_저장한다() {
    ArcRevision revision =
        ArcRevision.start(UUID.randomUUID(), 1, snapshot(), "arc-v1", UUID.randomUUID());

    assertThat(revision.getInputSnapshot()).contains("\"purchasedProductVariantIds\"");
    assertThat(revision.getInputSnapshot()).contains("\"preferredColors\":[\"BLACK\"]");
    assertThat(revision.getInputSnapshot()).contains("\"staffObservation\":\"재방문 시 신상품 안내\"");
  }

  @Test
  void 직원_관찰_메모는_200자를_초과할_수_없다() {
    assertThatThrownBy(
            () ->
                new ArcInputSnapshot(
                    List.of(UUID.randomUUID()),
                    Set.of(),
                    Set.of(),
                    null,
                    Set.of(),
                    null,
                    List.of(),
                    Set.of(),
                    null,
                    Set.of(),
                    Set.of(),
                    null,
                    "가".repeat(201)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("직원 관찰 메모는 200자를 초과할 수 없습니다.");
  }

  private ArcAndRevision sharedArc() {
    UUID staffId = UUID.randomUUID();
    Arc arc = Arc.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), staffId);
    ArcRevision revision = ArcRevision.start(arc.getId(), 1, snapshot(), "arc-v1", staffId);
    revision.complete(
        "{\"momentSummary\":\"My MCM\",\"preferences\":[\"디자인\"],\"momentToRemember\":\"첫 순간\"}",
        Instant.parse("2026-08-15T12:00:00Z"));
    arc.shareFirst(revision, Instant.parse("2026-08-15T12:01:00Z"), 1);
    return new ArcAndRevision(arc, revision);
  }

  private ArcRevision readyRevision(Arc arc, int revisionNumber, UUID staffId) {
    ArcRevision revision =
        ArcRevision.start(arc.getId(), revisionNumber, snapshot(), "arc-v1", staffId);
    revision.complete(
        """
        {"momentSummary":"요약","preferences":["선호"],"momentToRemember":"기억"}
        """,
        Instant.parse("2026-08-15T12:00:00Z"));
    return revision;
  }

  private record ArcAndRevision(Arc arc, ArcRevision revision) {}

  private ArcInputSnapshot snapshot() {
    return new ArcInputSnapshot(
        List.of(UUID.randomUUID()),
        Set.of(ProductCategory.BAG),
        Set.of(PreferredColor.BLACK),
        null,
        Set.of(PreferredStyle.CLASSIC_TIMELESS),
        null,
        List.of(UUID.randomUUID()),
        Set.of(PurchaseCriterion.DESIGN),
        null,
        Set.of(ActualInteractionPreference.MODERATE_GUIDANCE),
        Set.of(ProductExplanationPreference.KEY_POINTS_ONLY),
        PurchaseDecisionStyle.COMPARE_FIRST,
        "재방문 시 신상품 안내");
  }
}
