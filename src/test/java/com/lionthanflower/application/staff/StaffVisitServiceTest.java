// 직원 추천 고객의 응대 시작과 담당자 매칭 규칙을 검증하는 테스트
package com.lionthanflower.application.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.lionthanflower.application.staff.dto.StaffVisitAssignmentResponse;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.visit.entity.InteractionStyle;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import com.lionthanflower.domain.visit.error.VisitErrorCode;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import com.lionthanflower.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffVisitServiceTest {

  @Mock private VisitRepository visitRepository;
  @Mock private CustomerRepository customerRepository;

  private StaffVisitService service;
  private UUID storeId;

  @BeforeEach
  void setUp() {
    service = new StaffVisitService(visitRepository, customerRepository);
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

  private Staff staff(LanguageCode language) {
    return Staff.create(storeId, "김형진", "hashed-token", Set.of(language));
  }

  private Visit visit(InteractionStyle interactionStyle, LanguageCode language) {
    Visit visit = Visit.create(UUID.randomUUID(), storeId);
    visit.completeOnboarding(language, interactionStyle, null);
    return visit;
  }
}
