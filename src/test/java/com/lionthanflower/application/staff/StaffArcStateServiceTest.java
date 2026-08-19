// Arc 생성 상태 전이와 Solo 담당자 자동 배정을 검증하는 테스트
package com.lionthanflower.application.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lionthanflower.application.staff.dto.StaffArcGenerationRequest;
import com.lionthanflower.application.staff.dto.StaffArcRevisionResponse;
import com.lionthanflower.domain.arc.entity.ActualInteractionPreference;
import com.lionthanflower.domain.arc.entity.Arc;
import com.lionthanflower.domain.arc.entity.ArcGeneratedContent;
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
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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
    when(purchaseRepository.saveAndFlush(any(Purchase.class))).thenReturn(purchase);
    when(arcRepository.saveAndFlush(any(Arc.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
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
  void Arc와_구매의_유니크_제약_위반은_Arc_충돌로_변환한다() {
    Staff staff = staff();
    Visit visit = visit(UUID.randomUUID(), InteractionStyle.SELF_GUIDED);
    Customer customer = org.mockito.Mockito.mock(Customer.class);
    when(visitRepository.findByIdAndStoreId(visit.getId(), storeId)).thenReturn(Optional.of(visit));
    when(customerRepository.findById(visit.getCustomerId())).thenReturn(Optional.of(customer));
    when(arcRepository.findByVisitId(visit.getId())).thenReturn(Optional.empty());
    doThrow(uniqueConstraint("uk_purchases_visit_id"))
        .when(purchaseRepository)
        .saveAndFlush(any(Purchase.class));

    assertThatThrownBy(
            () ->
                service.prepareInitial(
                    visit.getId(), staff, new StaffArcGenerationRequest(snapshot())))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(ArcErrorCode.ALREADY_EXISTS));
  }

  @Test
  void 최초_Arc_생성_완료_시_고객에게_공개하고_방문을_완료한다() {
    Staff staff = staff();
    Visit visit = visit(UUID.randomUUID(), InteractionStyle.STAFF_RECOMMENDATION);
    visit.assignStaff(staff.getId(), Instant.now());
    visit.confirmPurchase(staff.getId(), Instant.now());
    Purchase purchase = Purchase.create(visit.getId());
    Arc arc = Arc.create(visit.getId(), purchase.getId(), visit.getCustomerId(), staff.getId());
    ArcRevision revision = ArcRevision.start(arc.getId(), 1, snapshot(), "arc-v1", staff.getId());
    ArcGeneratedContent content = new ArcGeneratedContent("오늘의 순간", List.of("실용성"), "기억할 순간");
    when(arcRevisionRepository.findById(revision.getId())).thenReturn(Optional.of(revision));
    when(arcRepository.findById(arc.getId())).thenReturn(Optional.of(arc));
    when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
    when(arcRepository.countByCustomerIdAndStatusIn(eq(visit.getCustomerId()), anyCollection()))
        .thenReturn(2L);
    when(arcRepository.saveAndFlush(arc)).thenReturn(arc);

    StaffArcRevisionResponse result = service.complete(revision.getId(), content);

    assertThat(result.arcStatus()).isEqualTo(ArcStatus.SHARED);
    assertThat(result.revisionStatus()).isEqualTo(ArcRevisionStatus.READY);
    assertThat(arc.getSharedRevisionId()).isEqualTo(revision.getId());
    assertThat(arc.getArcNumber()).isEqualTo(3);
    assertThat(visit.getStatus()).isEqualTo(VisitStatus.COMPLETED);
  }

  @Test
  void SHARED_Arc도_기존_입력으로_재생성할_수_있고_성공한_리비전으로_공개본을_교체한다() {
    Staff staff = staff();
    Visit visit = visit(UUID.randomUUID(), InteractionStyle.STAFF_RECOMMENDATION);
    visit.assignStaff(staff.getId(), Instant.now());
    visit.confirmPurchase(staff.getId(), Instant.now());
    Purchase purchase = Purchase.create(visit.getId());
    Arc arc = Arc.create(visit.getId(), purchase.getId(), visit.getCustomerId(), staff.getId());
    ArcRevision previous = ArcRevision.start(arc.getId(), 1, snapshot(), "arc-v1", staff.getId());
    previous.complete(
        "{\"momentSummary\":\"기존 순간\",\"preferences\":[\"실용성\"],\"momentToRemember\":\"기존 기억\"}",
        Instant.now());
    arc.shareFirst(previous, Instant.now(), 1);
    visit.complete(Instant.now());
    ArcRevision next = ArcRevision.start(arc.getId(), 2, snapshot(), "arc-v1", staff.getId());
    ArcInputSnapshot modifiedSnapshot = snapshot();
    Customer customer = org.mockito.Mockito.mock(Customer.class);
    when(customer.getName()).thenReturn("홍길동");
    when(arcRepository.findById(arc.getId())).thenReturn(Optional.of(arc));
    when(visitRepository.findByIdAndStoreId(visit.getId(), storeId)).thenReturn(Optional.of(visit));
    when(arcRevisionRepository.findTopByArcIdOrderByRevisionNumberDesc(arc.getId()))
        .thenReturn(Optional.of(previous));
    when(purchaseRepository.findByVisitId(visit.getId())).thenReturn(Optional.of(purchase));
    when(arcRevisionRepository.save(any(ArcRevision.class))).thenReturn(next);
    when(customerRepository.findById(visit.getCustomerId())).thenReturn(Optional.of(customer));
    when(arcRevisionRepository.findById(next.getId())).thenReturn(Optional.of(next));
    when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
    when(arcRepository.saveAndFlush(arc)).thenReturn(arc);

    StaffArcStateService.GenerationContext context =
        service.prepareRevision(
            arc.getId(), staff, new StaffArcGenerationRequest(modifiedSnapshot));
    StaffArcRevisionResponse result =
        service.complete(
            context.revisionId(), new ArcGeneratedContent("새 순간", List.of("실용성"), "새 기억"));

    assertThat(context.revisionId()).isEqualTo(next.getId());
    assertThat(context.generationCommand().inputSnapshot()).isEqualTo(modifiedSnapshot);
    assertThat(result.arcStatus()).isEqualTo(ArcStatus.SHARED);
    assertThat(arc.getSharedRevisionId()).isEqualTo(next.getId());
    assertThat(arc.getArcNumber()).isEqualTo(1);
    assertThat(visit.getStatus()).isEqualTo(VisitStatus.COMPLETED);
  }

  @Test
  void SHARED_Arc의_재생성_실패는_기존_공개본을_유지한다() {
    Staff staff = staff();
    Visit visit = visit(UUID.randomUUID(), InteractionStyle.STAFF_RECOMMENDATION);
    visit.assignStaff(staff.getId(), Instant.now());
    visit.confirmPurchase(staff.getId(), Instant.now());
    Purchase purchase = Purchase.create(visit.getId());
    Arc arc = Arc.create(visit.getId(), purchase.getId(), visit.getCustomerId(), staff.getId());
    ArcRevision previous = ArcRevision.start(arc.getId(), 1, snapshot(), "arc-v1", staff.getId());
    previous.complete(
        "{\"momentSummary\":\"기존 순간\",\"preferences\":[\"실용성\"],\"momentToRemember\":\"기존 기억\"}",
        Instant.now());
    arc.shareFirst(previous, Instant.now(), 1);
    ArcRevision failed = ArcRevision.start(arc.getId(), 2, snapshot(), "arc-v1", staff.getId());
    when(arcRevisionRepository.findById(failed.getId())).thenReturn(Optional.of(failed));
    when(arcRepository.findById(arc.getId())).thenReturn(Optional.of(arc));

    StaffArcRevisionResponse result = service.fail(failed.getId(), "OPENAI_GENERATION_FAILED");

    assertThat(result.revisionStatus()).isEqualTo(ArcRevisionStatus.FAILED);
    assertThat(arc.getStatus()).isEqualTo(ArcStatus.SHARED);
    assertThat(arc.getSharedRevisionId()).isEqualTo(previous.getId());
  }

  @Test
  void 구매_항목_삭제는_flush와_clear가_활성화된_bulk_query를_사용한다() throws Exception {
    Method method = PurchaseItemRepository.class.getMethod("deleteByPurchaseId", UUID.class);
    Modifying modifying = method.getAnnotation(Modifying.class);
    Query query = method.getAnnotation(Query.class);

    assertThat(modifying).isNotNull();
    assertThat(modifying.flushAutomatically()).isTrue();
    assertThat(modifying.clearAutomatically()).isTrue();
    assertThat(query).isNotNull();
    assertThat(query.value()).contains("delete from PurchaseItem");
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

  private DataIntegrityViolationException uniqueConstraint(String constraintName) {
    return new DataIntegrityViolationException(
        "유니크 제약 위반", new ConstraintViolationException("유니크 제약 위반", null, constraintName));
  }
}
