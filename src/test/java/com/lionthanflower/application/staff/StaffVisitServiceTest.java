// 직원 추천 고객의 응대 시작과 담당자 매칭 규칙을 검증하는 테스트
package com.lionthanflower.application.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lionthanflower.application.staff.dto.StaffVisitAssignmentResponse;
import com.lionthanflower.application.staff.dto.VisitResultType;
import com.lionthanflower.application.staff.dto.VisitSummaryResponse;
import com.lionthanflower.domain.arc.entity.Arc;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.visit.entity.InteractionStyle;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import com.lionthanflower.domain.visit.error.VisitErrorCode;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.infrastructure.persistence.ArcRepository;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import com.lionthanflower.infrastructure.persistence.VisitMemoryRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StaffVisitServiceTest {

  @Mock private VisitRepository visitRepository;
  @Mock private CustomerRepository customerRepository;
  @Mock private ArcRepository arcRepository;
  @Mock private VisitMemoryRepository visitMemoryRepository;

  private StaffVisitService service;
  private UUID storeId;

  @BeforeEach
  void setUp() {
    service =
        new StaffVisitService(
            visitRepository, customerRepository, arcRepository, visitMemoryRepository);
    storeId = UUID.randomUUID();
  }

  @Test
  void 언어가_달라도_직원_추천_대기_고객에게_응대를_시작한다() {
    Staff staff = staff(LanguageCode.EN);
    Visit visit = visit(InteractionStyle.STAFF_RECOMMENDATION, LanguageCode.JA);
    when(visitRepository.findByIdAndStoreId(visit.getId(), storeId)).thenReturn(Optional.of(visit));

    StaffVisitAssignmentResponse result = service.assignVisit(visit.getId(), staff);

    assertThat(result.visitId()).isEqualTo(visit.getId());
    assertThat(result.staffId()).isEqualTo(staff.getId());
    assertThat(result.status()).isEqualTo(VisitStatus.ACTIVE);
    assertThat(result.matchedAt()).isNotNull();
  }

  @Test
  void 다른_매장의_방문은_찾을_수_없음으로_처리한다() {
    Staff staff = staff(LanguageCode.EN);
    UUID visitId = UUID.randomUUID();
    when(visitRepository.findByIdAndStoreId(visitId, storeId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.assignVisit(visitId, staff))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception -> assertThat(exception.errorCode()).isEqualTo(VisitErrorCode.NOT_FOUND));
  }

  @Test
  void 이미_배정된_방문은_다시_매칭하지_않는다() {
    Staff staff = staff(LanguageCode.EN);
    Visit visit = visit(InteractionStyle.STAFF_RECOMMENDATION, LanguageCode.EN);
    visit.assignStaff(UUID.randomUUID(), Instant.now());
    when(visitRepository.findByIdAndStoreId(visit.getId(), storeId)).thenReturn(Optional.of(visit));

    assertThatThrownBy(() -> service.assignVisit(visit.getId(), staff))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(VisitErrorCode.NOT_ASSIGNABLE));
  }

  @Test
  void 혼자_보기_고객은_매칭하지_않는다() {
    Staff staff = staff(LanguageCode.EN);
    Visit visit = visit(InteractionStyle.SELF_GUIDED, LanguageCode.EN);
    when(visitRepository.findByIdAndStoreId(visit.getId(), storeId)).thenReturn(Optional.of(visit));

    assertThatThrownBy(() -> service.assignVisit(visit.getId(), staff))
        .isInstanceOfSatisfying(
            BusinessException.class,
            exception ->
                assertThat(exception.errorCode()).isEqualTo(VisitErrorCode.NOT_ASSIGNABLE));
  }

  @Test
  void 현재_방문_목록에_추가_요구사항과_고객의_Arc_개수를_포함한다() {
    Staff staff = staff(LanguageCode.EN);
    UUID customerId = UUID.randomUUID();
    Visit visit = Visit.create(customerId, storeId);
    Instant visitedAt = Instant.parse("2026-08-19T00:20:00Z");
    Instant matchedAt = Instant.parse("2026-08-19T00:24:00Z");
    visit.completeOnboarding(
        LanguageCode.EN, InteractionStyle.STAFF_RECOMMENDATION, "다양한 컬러를 보고 싶어요");
    visit.assignStaff(UUID.randomUUID(), matchedAt);
    ReflectionTestUtils.setField(visit, "createdAt", visitedAt);
    Customer customer = mock(Customer.class);
    Arc firstArc = mock(Arc.class);
    Arc secondArc = mock(Arc.class);
    when(customer.getId()).thenReturn(customerId);
    when(customer.getName()).thenReturn("홍길동");
    when(firstArc.getCustomerId()).thenReturn(customerId);
    when(secondArc.getCustomerId()).thenReturn(customerId);
    when(visitRepository.findByStoreIdAndStatusIn(
            org.mockito.ArgumentMatchers.eq(storeId), org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(visit));
    when(customerRepository.findAllById(Set.of(customerId))).thenReturn(List.of(customer));
    when(arcRepository.findByCustomerIdInAndStatusIn(
            org.mockito.ArgumentMatchers.eq(Set.of(customerId)),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(firstArc, secondArc));

    List<com.lionthanflower.application.staff.dto.VisitSummaryResponse> result =
        service.getCurrentVisits(storeId, staff.getId());

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().customerName()).isEqualTo("홍길동");
    assertThat(result.getFirst().additionalRequest()).isEqualTo("다양한 컬러를 보고 싶어요");
    assertThat(result.getFirst().arcCount()).isEqualTo(2);
    assertThat(result.getFirst())
        .hasFieldOrPropertyWithValue("matchedAt", matchedAt)
        .hasFieldOrPropertyWithValue("visitedAt", visitedAt);
  }

  @Test
  void 현재_방문_목록에_방문별_Arc와_Visit_Memory_ID를_포함한다() {
    Staff staff = staff(LanguageCode.EN);
    UUID customerId = UUID.randomUUID();
    Visit visit = visit(InteractionStyle.STAFF_RECOMMENDATION, LanguageCode.EN);
    Arc arc = mock(Arc.class);
    UUID arcId = UUID.randomUUID();
    com.lionthanflower.domain.visitmemory.entity.VisitMemory visitMemory =
        mock(com.lionthanflower.domain.visitmemory.entity.VisitMemory.class);
    UUID visitMemoryId = UUID.randomUUID();
    Customer customer = mock(Customer.class);

    when(customer.getId()).thenReturn(customerId);
    when(customer.getName()).thenReturn("홍길동");
    when(arc.getId()).thenReturn(arcId);
    when(arc.getVisitId()).thenReturn(visit.getId());
    when(arc.getCustomerId()).thenReturn(customerId);
    when(visitMemory.getId()).thenReturn(visitMemoryId);
    when(visitMemory.getVisitId()).thenReturn(visit.getId());
    when(visitRepository.findByStoreIdAndStatusIn(
            org.mockito.ArgumentMatchers.eq(storeId), org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(visit));
    when(customerRepository.findAllById(Set.of(visit.getCustomerId())))
        .thenReturn(List.of(customer));
    when(arcRepository.findByCustomerIdInAndStatusIn(
            org.mockito.ArgumentMatchers.eq(Set.of(visit.getCustomerId())),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(arc));
    when(arcRepository.findByVisitIdIn(Set.of(visit.getId()))).thenReturn(List.of(arc));
    when(visitMemoryRepository.findByVisitIdIn(Set.of(visit.getId())))
        .thenReturn(List.of(visitMemory));

    List<com.lionthanflower.application.staff.dto.VisitSummaryResponse> result =
        service.getCurrentVisits(storeId, staff.getId());

    assertThat(result.getFirst().arcId()).isEqualTo(arcId);
    assertThat(result.getFirst().visitMemoryId()).isEqualTo(visitMemoryId);
  }

  @Test
  void 매장_진행_방문과_현재_직원의_완료_방문을_최신순으로_반환하고_결과를_매핑한다() {
    Staff staff = staff(LanguageCode.EN);
    Visit oldest = visit(InteractionStyle.SELF_GUIDED, LanguageCode.EN);
    Visit newest = visit(InteractionStyle.STAFF_RECOMMENDATION, LanguageCode.EN);
    Visit completed = visit(InteractionStyle.SELF_GUIDED, LanguageCode.EN);
    Instant oldestAt = Instant.parse("2026-08-18T00:00:00Z");
    Instant newestAt = Instant.parse("2026-08-20T00:00:00Z");
    Instant completedAt = Instant.parse("2026-08-19T00:00:00Z");
    Instant matchedAt = Instant.parse("2026-08-18T00:05:00Z");
    completed.assignStaff(staff.getId(), matchedAt);
    completed.confirmPurchase(staff.getId(), Instant.parse("2026-08-18T01:00:00Z"));
    completed.complete(completedAt);
    ReflectionTestUtils.setField(oldest, "createdAt", oldestAt);
    ReflectionTestUtils.setField(newest, "createdAt", newestAt);
    ReflectionTestUtils.setField(completed, "createdAt", completedAt);

    Arc completedArc = mock(Arc.class);
    UUID completedArcId = UUID.randomUUID();
    when(completedArc.getId()).thenReturn(completedArcId);
    when(completedArc.getVisitId()).thenReturn(completed.getId());
    when(completedArc.getArcNumber()).thenReturn(2);
    when(visitRepository.findByStoreIdAndStatusIn(
            storeId,
            Set.of(
                VisitStatus.WAITING_FOR_STAFF,
                VisitStatus.ACTIVE,
                VisitStatus.ARC_IN_PROGRESS,
                VisitStatus.VISIT_MEMORY_IN_PROGRESS)))
        .thenReturn(List.of(oldest, newest));
    when(visitRepository.findByStoreIdAndStaffIdAndStatus(
            storeId, staff.getId(), VisitStatus.COMPLETED))
        .thenReturn(List.of(completed));
    when(customerRepository.findAllById(
            Set.of(oldest.getCustomerId(), newest.getCustomerId(), completed.getCustomerId())))
        .thenReturn(List.of());
    when(arcRepository.findByCustomerIdInAndStatusIn(
            org.mockito.ArgumentMatchers.anySet(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of());
    when(arcRepository.findByVisitIdIn(Set.of(oldest.getId(), newest.getId(), completed.getId())))
        .thenReturn(List.of(completedArc));
    when(visitMemoryRepository.findByVisitIdIn(
            Set.of(oldest.getId(), newest.getId(), completed.getId())))
        .thenReturn(List.of());

    List<VisitSummaryResponse> result = service.getCurrentVisits(storeId, staff.getId());

    assertThat(result)
        .extracting(VisitSummaryResponse::visitId)
        .containsExactly(newest.getId(), completed.getId(), oldest.getId());
    assertThat(result.get(1).resultType()).isEqualTo(VisitResultType.ARC);
    assertThat(result.get(1).resultId()).isEqualTo(completedArcId);
    assertThat(result.get(1).arcNumber()).isEqualTo(2);
    assertThat(result.get(1).completedAt()).isEqualTo(completedAt);
  }

  @Test
  void 미구매로_완료된_방문은_Visit_Memory_결과로_매핑한다() {
    Staff staff = staff(LanguageCode.EN);
    Visit completed = visit(InteractionStyle.SELF_GUIDED, LanguageCode.EN);
    Instant matchedAt = Instant.parse("2026-08-18T00:05:00Z");
    Instant completedAt = Instant.parse("2026-08-18T01:00:00Z");
    completed.assignStaff(staff.getId(), matchedAt);
    completed.confirmNoPurchase(staff.getId(), matchedAt);
    completed.complete(completedAt);
    ReflectionTestUtils.setField(completed, "createdAt", completedAt);

    com.lionthanflower.domain.visitmemory.entity.VisitMemory visitMemory =
        mock(com.lionthanflower.domain.visitmemory.entity.VisitMemory.class);
    UUID visitMemoryId = UUID.randomUUID();
    when(visitMemory.getId()).thenReturn(visitMemoryId);
    when(visitMemory.getVisitId()).thenReturn(completed.getId());
    when(visitRepository.findByStoreIdAndStatusIn(
            storeId,
            Set.of(
                VisitStatus.WAITING_FOR_STAFF,
                VisitStatus.ACTIVE,
                VisitStatus.ARC_IN_PROGRESS,
                VisitStatus.VISIT_MEMORY_IN_PROGRESS)))
        .thenReturn(List.of());
    when(visitRepository.findByStoreIdAndStaffIdAndStatus(
            storeId, staff.getId(), VisitStatus.COMPLETED))
        .thenReturn(List.of(completed));
    when(customerRepository.findAllById(Set.of(completed.getCustomerId()))).thenReturn(List.of());
    when(arcRepository.findByCustomerIdInAndStatusIn(
            org.mockito.ArgumentMatchers.anySet(), org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of());
    when(arcRepository.findByVisitIdIn(Set.of(completed.getId()))).thenReturn(List.of());
    when(visitMemoryRepository.findByVisitIdIn(Set.of(completed.getId())))
        .thenReturn(List.of(visitMemory));

    VisitSummaryResponse result = service.getCurrentVisits(storeId, staff.getId()).getFirst();

    assertThat(result.resultType()).isEqualTo(VisitResultType.VISIT_MEMORY);
    assertThat(result.resultId()).isEqualTo(visitMemoryId);
    assertThat(result.visitMemoryId()).isEqualTo(visitMemoryId);
    assertThat(result.arcNumber()).isNull();
    assertThat(result.completedAt()).isEqualTo(completedAt);
  }

  @Test
  void 온보딩_완료_전_고객은_현재_방문_목록에서_제외한다() {
    when(visitRepository.findByStoreIdAndStatusIn(
            org.mockito.ArgumentMatchers.eq(storeId), org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of());

    List<com.lionthanflower.application.staff.dto.VisitSummaryResponse> result =
        service.getCurrentVisits(storeId, UUID.randomUUID());

    assertThat(result).isEmpty();
    verify(visitRepository)
        .findByStoreIdAndStatusIn(
            storeId,
            Set.of(
                VisitStatus.WAITING_FOR_STAFF,
                VisitStatus.ACTIVE,
                VisitStatus.ARC_IN_PROGRESS,
                VisitStatus.VISIT_MEMORY_IN_PROGRESS));
  }

  private Staff staff(LanguageCode language) {
    return Staff.create(storeId, "김형진", "hashed-token", Set.of(language));
  }

  private Visit visit(InteractionStyle interactionStyle, LanguageCode language) {
    Visit visit = Visit.create(UUID.randomUUID(), storeId);
    visit.completeOnboarding(language, interactionStyle, null);
    return visit;
  }
}
