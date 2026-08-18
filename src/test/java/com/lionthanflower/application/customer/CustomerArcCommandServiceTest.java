// 고객 Arc 최종 저장 Application Service를 검증하는 단위 테스트
package com.lionthanflower.application.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lionthanflower.domain.arc.entity.ActualInteractionPreference;
import com.lionthanflower.domain.arc.entity.Arc;
import com.lionthanflower.domain.arc.entity.ArcRevision;
import com.lionthanflower.domain.arc.entity.ArcStatus;
import com.lionthanflower.domain.arc.entity.PreferredColor;
import com.lionthanflower.domain.arc.entity.PreferredStyle;
import com.lionthanflower.domain.arc.entity.ProductExplanationPreference;
import com.lionthanflower.domain.arc.entity.PurchaseCriterion;
import com.lionthanflower.domain.arc.entity.PurchaseDecisionStyle;
import com.lionthanflower.domain.arc.error.ArcErrorCode;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.product.entity.ProductCategory;
import com.lionthanflower.domain.visit.entity.InteractionStyle;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.error.CommonErrorCode;
import com.lionthanflower.infrastructure.persistence.ArcRepository;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import com.lionthanflower.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerArcCommandServiceTest {

  @Mock private CustomerRepository customerRepository;
  @Mock private ArcRepository arcRepository;
  @Mock private VisitRepository visitRepository;

  private CustomerTokenManager tokenManager;
  private CustomerArcCommandService service;

  @BeforeEach
  void setUp() {
    tokenManager = new CustomerTokenManager();
    service =
        new CustomerArcCommandService(
            customerRepository, arcRepository, visitRepository, tokenManager);
  }

  @Test
  void 공유된_Arc를_최종_저장하고_방문을_완료한다() {
    String rawToken = "known-token";
    Customer customer = Customer.create(tokenManager.hash(rawToken));
    UUID staffId = UUID.randomUUID();
    Visit visit = activePurchasedVisit(customer.getId(), staffId);
    Arc arc = sharedArc(visit, customer.getId(), staffId);
    when(customerRepository.findByTokenHash(tokenManager.hash(rawToken)))
        .thenReturn(Optional.of(customer));
    when(arcRepository.findById(arc.getId())).thenReturn(Optional.of(arc));
    when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

    CustomerArcCommandService.ArcFinalization result = service.finalizeArc(arc.getId(), rawToken);

    assertThat(result.arcId()).isEqualTo(arc.getId());
    assertThat(result.status()).isEqualTo(ArcStatus.FINALIZED);
    assertThat(arc.getStatus()).isEqualTo(ArcStatus.FINALIZED);
    assertThat(visit.getStatus()).isEqualTo(VisitStatus.COMPLETED);
  }

  @Test
  void 이미_최종_저장된_Arc는_멱등하게_반환한다() {
    String rawToken = "known-token";
    Customer customer = Customer.create(tokenManager.hash(rawToken));
    UUID staffId = UUID.randomUUID();
    Visit visit = activePurchasedVisit(customer.getId(), staffId);
    Arc arc = sharedArc(visit, customer.getId(), staffId);
    arc.finalizeSharedRevision(Instant.parse("2026-08-15T12:05:00Z"));
    visit.complete(Instant.parse("2026-08-15T12:05:00Z"));
    when(customerRepository.findByTokenHash(tokenManager.hash(rawToken)))
        .thenReturn(Optional.of(customer));
    when(arcRepository.findById(arc.getId())).thenReturn(Optional.of(arc));

    CustomerArcCommandService.ArcFinalization result = service.finalizeArc(arc.getId(), rawToken);

    assertThat(result.status()).isEqualTo(ArcStatus.FINALIZED);
    assertThat(arc.getFinalizedAt()).isEqualTo(Instant.parse("2026-08-15T12:05:00Z"));
  }

  @Test
  void 다른_고객의_Arc는_찾을_수_없는_것처럼_처리한다() {
    String rawToken = "known-token";
    Customer customer = Customer.create(tokenManager.hash(rawToken));
    Arc arc =
        Arc.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    when(customerRepository.findByTokenHash(tokenManager.hash(rawToken)))
        .thenReturn(Optional.of(customer));
    when(arcRepository.findById(arc.getId())).thenReturn(Optional.of(arc));

    assertThatThrownBy(() -> service.finalizeArc(arc.getId(), rawToken))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(CommonErrorCode.NOT_FOUND);
  }

  @Test
  void 공유되지_않은_Arc는_최종_저장할_수_없다() {
    String rawToken = "known-token";
    Customer customer = Customer.create(tokenManager.hash(rawToken));
    Arc arc = Arc.create(UUID.randomUUID(), UUID.randomUUID(), customer.getId(), UUID.randomUUID());
    when(customerRepository.findByTokenHash(tokenManager.hash(rawToken)))
        .thenReturn(Optional.of(customer));
    when(arcRepository.findById(arc.getId())).thenReturn(Optional.of(arc));

    assertThatThrownBy(() -> service.finalizeArc(arc.getId(), rawToken))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(ArcErrorCode.NOT_ASSIGNABLE);
  }

  @Test
  void 고객_토큰이_없으면_인증_오류를_반환한다() {
    assertThatThrownBy(() -> service.finalizeArc(UUID.randomUUID(), null))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(CommonErrorCode.UNAUTHORIZED);
  }

  private Visit activePurchasedVisit(UUID customerId, UUID staffId) {
    Visit visit = Visit.create(customerId, UUID.randomUUID());
    visit.completeOnboarding(LanguageCode.EN, InteractionStyle.STAFF_RECOMMENDATION, null);
    visit.assignStaff(staffId, Instant.parse("2026-08-15T12:00:00Z"));
    visit.confirmPurchase(staffId, Instant.parse("2026-08-15T12:01:00Z"));
    return visit;
  }

  private Arc sharedArc(Visit visit, UUID customerId, UUID staffId) {
    Arc arc = Arc.create(visit.getId(), UUID.randomUUID(), customerId, staffId);
    ArcRevision revision = ArcRevision.start(arc.getId(), 1, snapshot(), "arc-v1", staffId);
    revision.complete(
        "{\"momentSummary\":\"요약\",\"preferences\":[\"선호\"],\"momentToRemember\":\"기억\"}",
        Instant.parse("2026-08-15T12:02:00Z"));
    arc.shareFirst(revision, Instant.parse("2026-08-15T12:03:00Z"), 1);
    return arc;
  }

  private com.lionthanflower.domain.arc.entity.ArcInputSnapshot snapshot() {
    return new com.lionthanflower.domain.arc.entity.ArcInputSnapshot(
        LocalDate.of(2026, 8, 15),
        "KR",
        "MCM HAUS",
        List.of(UUID.randomUUID()),
        Set.of(ProductCategory.BAG),
        Set.of(PreferredColor.BLACK),
        null,
        Set.of(PreferredStyle.CLASSIC_TIMELESS),
        null,
        List.of(),
        Set.of(PurchaseCriterion.DESIGN),
        null,
        Set.of(ActualInteractionPreference.MODERATE_GUIDANCE),
        Set.of(ProductExplanationPreference.KEY_POINTS_ONLY),
        PurchaseDecisionStyle.COMPARE_FIRST,
        null);
  }
}
