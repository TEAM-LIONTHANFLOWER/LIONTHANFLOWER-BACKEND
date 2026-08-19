// 고객 서비스 진입과 온보딩 진행 Application Service를 검증하는 테스트
package com.lionthanflower.application.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.store.entity.Store;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.store.repository.StaffRepository;
import com.lionthanflower.domain.visit.entity.InteractionStyle;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.error.CommonErrorCode;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import com.lionthanflower.infrastructure.persistence.StoreRepository;
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
class CustomerVisitServiceTest {

  private static final String STORE_CODE = "MCM-SEOUL";

  @Mock private CustomerRepository customerRepository;
  @Mock private StoreRepository storeRepository;
  @Mock private VisitRepository visitRepository;
  @Mock private StaffRepository staffRepository;

  private CustomerTokenManager tokenManager;
  private CustomerVisitService service;
  private Store store;

  @BeforeEach
  void setUp() {
    tokenManager = new CustomerTokenManager();
    service =
        new CustomerVisitService(
            customerRepository,
            storeRepository,
            visitRepository,
            staffRepository,
            tokenManager,
            STORE_CODE);
    store = Store.create("MCM Seoul", STORE_CODE, "KR");
  }

  @Test
  void 기존_고객을_재사용하고_진입할_때마다_새_방문을_생성한다() {
    Customer customer = Customer.create(tokenManager.hash("known-token"));
    customer.updateName("홍길동");
    when(storeRepository.findByCode(STORE_CODE)).thenReturn(Optional.of(store));
    when(customerRepository.findByTokenHash(tokenManager.hash("known-token")))
        .thenReturn(Optional.of(customer));
    when(visitRepository.save(any(Visit.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CustomerVisitService.EntryResult result = service.enter("known-token");

    assertThat(result.customerName()).isEqualTo("홍길동");
    assertThat(result.status()).isEqualTo(VisitStatus.ONBOARDING);
    assertThat(result.issuedToken()).isNull();
    verify(customerRepository, never()).save(any(Customer.class));
    verify(visitRepository).save(any(Visit.class));
  }

  @Test
  void 쿠키가_없으면_새_고객과_방문을_생성하고_새_토큰을_발급한다() {
    when(storeRepository.findByCode(STORE_CODE)).thenReturn(Optional.of(store));
    when(customerRepository.save(any(Customer.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(visitRepository.save(any(Visit.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CustomerVisitService.EntryResult result = service.enter(null);

    assertThat(result.issuedToken()).isNotBlank();
    assertThat(result.status()).isEqualTo(VisitStatus.ONBOARDING);
    verify(customerRepository).save(any(Customer.class));
    verify(visitRepository).save(any(Visit.class));
  }

  @Test
  void 알_수_없는_쿠키는_새_고객으로_교체한다() {
    when(storeRepository.findByCode(STORE_CODE)).thenReturn(Optional.of(store));
    when(customerRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
    when(customerRepository.save(any(Customer.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(visitRepository.save(any(Visit.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    CustomerVisitService.EntryResult result = service.enter("unknown-token");

    assertThat(result.issuedToken()).isNotBlank().isNotEqualTo("unknown-token");
    verify(customerRepository).save(any(Customer.class));
    verify(visitRepository).save(any(Visit.class));
  }

  @Test
  void 설정된_매장이_없으면_서버_오류를_반환한다() {
    when(storeRepository.findByCode(STORE_CODE)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.enter(null))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR);
  }

  @Test
  void 직원_추천_온보딩은_고객_이름을_저장하고_대기_상태로_전환한다() {
    Customer customer = Customer.create(tokenManager.hash("known-token"));
    Visit visit = Visit.create(customer.getId(), store.getId());
    when(customerRepository.findByTokenHash(tokenManager.hash("known-token")))
        .thenReturn(Optional.of(customer));
    when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

    CustomerVisitService.OnboardingResult result =
        service.progressOnboarding(
            visit.getId(),
            "known-token",
            new CustomerVisitService.OnboardingCommand(
                "홍길동", LanguageCode.EN, InteractionStyle.STAFF_RECOMMENDATION, "가방 추천"));

    assertThat(customer.getName()).isEqualTo("홍길동");
    assertThat(result.visitId()).isEqualTo(visit.getId());
    assertThat(result.status()).isEqualTo(VisitStatus.WAITING_FOR_STAFF);
  }

  @Test
  void 셀프_이용_온보딩은_ACTIVE_상태로_전환한다() {
    Customer customer = Customer.create(tokenManager.hash("known-token"));
    Visit visit = Visit.create(customer.getId(), store.getId());
    when(customerRepository.findByTokenHash(tokenManager.hash("known-token")))
        .thenReturn(Optional.of(customer));
    when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

    CustomerVisitService.OnboardingResult result =
        service.progressOnboarding(
            visit.getId(),
            "known-token",
            new CustomerVisitService.OnboardingCommand(
                "홍길동", LanguageCode.JA, InteractionStyle.SELF_GUIDED, null));

    assertThat(result.status()).isEqualTo(VisitStatus.ACTIVE);
  }

  @Test
  void 고객_토큰이_없으면_온보딩을_진행할_수_없다() {
    Visit visit = Visit.create(java.util.UUID.randomUUID(), store.getId());

    assertThatThrownBy(
            () ->
                service.progressOnboarding(
                    visit.getId(),
                    null,
                    new CustomerVisitService.OnboardingCommand(
                        "홍길동", LanguageCode.EN, InteractionStyle.SELF_GUIDED, null)))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE);
  }

  @Test
  void 다른_고객의_방문은_찾을_수_없는_것처럼_처리한다() {
    Customer customer = Customer.create(tokenManager.hash("known-token"));
    Visit visit = Visit.create(java.util.UUID.randomUUID(), store.getId());
    when(customerRepository.findByTokenHash(tokenManager.hash("known-token")))
        .thenReturn(Optional.of(customer));
    when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

    assertThatThrownBy(
            () ->
                service.progressOnboarding(
                    visit.getId(),
                    "known-token",
                    new CustomerVisitService.OnboardingCommand(
                        "홍길동", LanguageCode.EN, InteractionStyle.SELF_GUIDED, null)))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(CommonErrorCode.NOT_FOUND);
  }

  @Test
  void 이미_온보딩이_끝난_방문은_다시_진행할_수_없다() {
    Customer customer = Customer.create(tokenManager.hash("known-token"));
    Visit visit = Visit.create(customer.getId(), store.getId());
    visit.completeOnboarding(LanguageCode.EN, InteractionStyle.SELF_GUIDED, null);
    when(customerRepository.findByTokenHash(tokenManager.hash("known-token")))
        .thenReturn(Optional.of(customer));
    when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

    assertThatThrownBy(
            () ->
                service.progressOnboarding(
                    visit.getId(),
                    "known-token",
                    new CustomerVisitService.OnboardingCommand(
                        "홍길동", LanguageCode.EN, InteractionStyle.SELF_GUIDED, null)))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE);
  }

  @Test
  void 고객은_직원_추천_방문의_대기_상태를_조회한다() {
    Customer customer = Customer.create(tokenManager.hash("known-token"));
    Visit visit = Visit.create(customer.getId(), store.getId());
    visit.completeOnboarding(LanguageCode.KO, InteractionStyle.STAFF_RECOMMENDATION, null);
    when(customerRepository.findByTokenHash(tokenManager.hash("known-token")))
        .thenReturn(Optional.of(customer));
    when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));

    CustomerVisitService.MatchingResult result =
        service.getMatching(visit.getId(), "known-token");

    assertThat(result.status()).isEqualTo(VisitStatus.WAITING_FOR_STAFF);
    assertThat(result.staffId()).isNull();
    assertThat(result.staffName()).isNull();
    assertThat(result.matchedAt()).isNull();
  }

  @Test
  void 고객은_배정된_직원과_매칭_시각을_조회한다() {
    Customer customer = Customer.create(tokenManager.hash("known-token"));
    Visit visit = Visit.create(customer.getId(), store.getId());
    visit.completeOnboarding(LanguageCode.KO, InteractionStyle.STAFF_RECOMMENDATION, null);
    Staff staff =
        Staff.create(store.getId(), "김형진", "hashed-token", Set.of(LanguageCode.KO));
    Instant matchedAt = Instant.parse("2026-08-19T01:00:00Z");
    visit.assignStaff(staff.getId(), matchedAt);
    when(customerRepository.findByTokenHash(tokenManager.hash("known-token")))
        .thenReturn(Optional.of(customer));
    when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
    when(staffRepository.findById(staff.getId())).thenReturn(Optional.of(staff));

    CustomerVisitService.MatchingResult result =
        service.getMatching(visit.getId(), "known-token");

    assertThat(result.status()).isEqualTo(VisitStatus.ACTIVE);
    assertThat(result.staffId()).isEqualTo(staff.getId());
    assertThat(result.staffName()).isEqualTo("김형진");
    assertThat(result.matchedAt()).isEqualTo(matchedAt);
  }

  @Test
  void 고객_토큰이_없으면_매칭_상태를_조회할_수_없다() {
    assertThatThrownBy(() -> service.getMatching(UUID.randomUUID(), null))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(CommonErrorCode.INVALID_INPUT_VALUE);
  }

  @Test
  void 다른_고객의_매칭_상태는_찾을_수_없는_것처럼_처리한다() {
    Customer customer = Customer.create(tokenManager.hash("known-token"));
    Visit otherVisit = Visit.create(UUID.randomUUID(), store.getId());
    when(customerRepository.findByTokenHash(tokenManager.hash("known-token")))
        .thenReturn(Optional.of(customer));
    when(visitRepository.findById(otherVisit.getId())).thenReturn(Optional.of(otherVisit));

    assertThatThrownBy(() -> service.getMatching(otherVisit.getId(), "known-token"))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(CommonErrorCode.NOT_FOUND);
  }
}
