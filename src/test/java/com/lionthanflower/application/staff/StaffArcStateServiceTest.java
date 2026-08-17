// Arc 생성 상태 전이와 Solo 담당자 자동 배정을 검증하는 테스트
package com.lionthanflower.application.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lionthanflower.application.staff.dto.StaffArcGenerationRequest;
import com.lionthanflower.application.staff.dto.StaffArcRevisionResponse;
import com.lionthanflower.domain.arc.entity.ActualInteractionPreference;
import com.lionthanflower.domain.arc.entity.Arc;
import com.lionthanflower.domain.arc.entity.ArcInputSnapshot;
import com.lionthanflower.domain.arc.entity.ArcRevision;
import com.lionthanflower.domain.arc.entity.ArcRevisionStatus;
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
import com.lionthanflower.domain.purchase.entity.Purchase;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.visit.entity.InteractionStyle;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.infrastructure.persistence.ArcRepository;
import com.lionthanflower.infrastructure.persistence.ArcRevisionRepository;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import com.lionthanflower.infrastructure.persistence.PurchaseItemRepository;
import com.lionthanflower.infrastructure.persistence.PurchaseRepository;
import com.lionthanflower.infrastructure.persistence.VisitRepository;
import java.time.Instant;
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
class StaffArcStateServiceTest {

  @Mock private VisitRepository visitRepository;
  @Mock private CustomerRepository customerRepository;
  @Mock private PurchaseRepository purchaseRepository;
  @Mock private PurchaseItemRepository purchaseItemRepository;
  @Mock private ArcRepository arcRepository;
  @Mock private ArcRevisionRepository arcRevisionRepository;

  private StaffArcStateService service;
  private UUID storeId;

  @BeforeEach
  void setUp() {
    service =
        new StaffArcStateService(
            visitRepository,
            customerRepository,
            purchaseRepository,
            purchaseItemRepository,
            arcRepository,
            arcRevisionRepository,
            "arc-v1");
    storeId = UUID.randomUUID();
  }

  @Test
  void Solo_방문은_Arc_생성_요청_직원을_담당자로_자동_배정한다() {
    Staff staff = staff();
    UUID customerId = UUID.randomUUID();
    Visit visit = visit(customerId, InteractionStyle.SELF_GUIDED);
    Customer customer = customer(customerId, "홍길동");
    Purchase purchase = Purchase.create(visit.getId());
    when(visitRepository.findByIdAndStoreId(visit.getId(), storeId)).thenReturn(Optional.of(visit));
    when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
    when(arcRepository.findByVisitId(visit.getId())).thenReturn(Optional.empty());
    when(purchaseRepository.save(any(Purchase.class))).thenReturn(purchase);
    when(arcRepository.save(any(Arc.class))).thenAnswer(invocation -> invocation.getArgument(0));
    when(arcRevisionRepository.save(any(ArcRevision.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    StaffArcStateService.GenerationContext context =
        service.prepareInitial(visit.getId(), staff, new StaffArcGenerationRequest(snapshot()));

    assertThat(visit.getStaffId()).isEqualTo(staff.getId());
    assertThat(visit.getStatus()).isEqualTo(VisitStatus.ARC_IN_PROGRESS);
    assertThat(context.generationCommand().customerName()).isEqualTo("홍길동");
    verify(purchaseItemRepository).saveAll(anyCollection());
  }

  @Test
  void 이미_Arc가_있는_방문은_최초_생성을_중복_처리하지_않는다() {
    Staff staff = staff();
    Visit visit = visit(UUID.randomUUID(), InteractionStyle.STAFF_RECOMMENDATION);
    visit.assignStaff(staff.getId(), Instant.now());
    Arc existing =
        Arc.create(visit.getId(), UUID.randomUUID(), visit.getCustomerId(), staff.getId());
    when(visitRepository.findByIdAndStoreId(visit.getId(), storeId)).thenReturn(Optional.of(visit));
    when(arcRepository.findByVisitId(visit.getId())).thenReturn(Optional.of(existing));

    assertThatThrownBy(
            () ->
                service.prepareInitial(
                    visit.getId(), staff, new StaffArcGenerationRequest(snapshot())))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ArcErrorCode.ALREADY_EXISTS));
  }

  @Test
  void READY_리비전을_공유하면_Arc를_SHARED로_전환한다() {
    Staff staff = staff();
    Visit visit = visit(UUID.randomUUID(), InteractionStyle.STAFF_RECOMMENDATION);
    visit.assignStaff(staff.getId(), Instant.now());
    visit.confirmPurchase(staff.getId(), Instant.now());
    Purchase purchase = Purchase.create(visit.getId());
    Arc arc = Arc.create(visit.getId(), purchase.getId(), visit.getCustomerId(), staff.getId());
    ArcRevision revision = ArcRevision.start(arc.getId(), 1, snapshot(), "arc-v1", staff.getId());
    revision.complete(
        "{\"momentSummary\":\"오늘의 순간\",\"preferences\":[\"실용성\"],\"momentToRemember\":\"기억할 순간\"}",
        Instant.now());
    when(arcRepository.findById(arc.getId())).thenReturn(Optional.of(arc));
    when(visitRepository.findByIdAndStoreId(visit.getId(), storeId)).thenReturn(Optional.of(visit));
    when(arcRevisionRepository.findByIdAndArcId(revision.getId(), arc.getId()))
        .thenReturn(Optional.of(revision));
    when(arcRepository.countByCustomerIdAndStatusIn(eq(visit.getCustomerId()), anyCollection()))
        .thenReturn(0L);

    StaffArcRevisionResponse result = service.share(arc.getId(), revision.getId(), staff);

    assertThat(result.arcStatus()).isEqualTo(ArcStatus.SHARED);
    assertThat(result.revisionStatus()).isEqualTo(ArcRevisionStatus.READY);
    assertThat(arc.getArcNumber()).isEqualTo(1);
  }

  private Staff staff() {
    return Staff.create(storeId, "김형진", "hashed-token", Set.of(LanguageCode.EN));
  }

  private Visit visit(UUID customerId, InteractionStyle interactionStyle) {
    Visit visit = Visit.create(customerId, storeId);
    visit.completeOnboarding(LanguageCode.EN, interactionStyle, "컬러 요청");
    return visit;
  }

  private Customer customer(UUID customerId, String name) {
    Customer customer = org.mockito.Mockito.mock(Customer.class);
    when(customer.getId()).thenReturn(customerId);
    when(customer.getName()).thenReturn(name);
    return customer;
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
