// 직원 Visit Memory 상태 전이와 방문 완료·알림 트랜잭션을 검증하는 테스트
package com.lionthanflower.application.staff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lionthanflower.application.staff.dto.StaffVisitMemoryGenerationRequest;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.notification.entity.CustomerNotification;
import com.lionthanflower.domain.product.entity.ProductVariant;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.visit.entity.InteractionStyle;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import com.lionthanflower.domain.visitmemory.entity.CustomerInterestPoint;
import com.lionthanflower.domain.visitmemory.entity.NoPurchaseReason;
import com.lionthanflower.domain.visitmemory.entity.ProductEngagement;
import com.lionthanflower.domain.visitmemory.entity.VisitMemory;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryInputSnapshot;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryStatus;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.infrastructure.persistence.CustomerNotificationRepository;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import com.lionthanflower.infrastructure.persistence.ProductVariantRepository;
import com.lionthanflower.infrastructure.persistence.VisitMemoryRepository;
import com.lionthanflower.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffVisitMemoryStateServiceTest {

  @Mock private VisitRepository visitRepository;
  @Mock private CustomerRepository customerRepository;
  @Mock private ProductVariantRepository productVariantRepository;
  @Mock private VisitMemoryRepository visitMemoryRepository;
  @Mock private CustomerNotificationRepository customerNotificationRepository;

  private StaffVisitMemoryStateService service;
  private UUID storeId;

  @BeforeEach
  void setUp() {
    service =
        new StaffVisitMemoryStateService(
            visitRepository,
            customerRepository,
            productVariantRepository,
            visitMemoryRepository,
            customerNotificationRepository,
            "visit-memory-v1");
    storeId = UUID.randomUUID();
  }

  @Test
  void Solo_방문은_Visit_Memory_생성_직원을_담당자로_자동_배정한다() {
    Staff staff = staff();
    Visit visit = visit(InteractionStyle.SELF_GUIDED);
    Customer customer = customer(visit.getCustomerId(), "홍길동");
    when(visitRepository.findByIdAndStoreId(visit.getId(), storeId)).thenReturn(Optional.of(visit));
    when(customerRepository.findById(visit.getCustomerId())).thenReturn(Optional.of(customer));
    when(visitMemoryRepository.findByVisitId(visit.getId())).thenReturn(Optional.empty());
    when(productVariantRepository.findAllById(anyCollection()))
        .thenReturn(java.util.List.of(org.mockito.Mockito.mock(ProductVariant.class)));
    when(visitMemoryRepository.saveAndFlush(any(VisitMemory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    StaffVisitMemoryStateService.GenerationContext context =
        service.prepareInitial(
            visit.getId(), staff, new StaffVisitMemoryGenerationRequest(snapshot()));

    assertThat(visit.getStaffId()).isEqualTo(staff.getId());
    assertThat(visit.getStatus()).isEqualTo(VisitStatus.VISIT_MEMORY_IN_PROGRESS);
    assertThat(context.generationCommand().customerName()).isEqualTo("홍길동");
  }

  @Test
  void Visit_Memory를_공유하면_방문을_완료하고_고객_알림을_저장한다() {
    Staff staff = staff();
    Visit visit = visit(InteractionStyle.SELF_GUIDED);
    visit.assignStaff(staff.getId(), Instant.now());
    visit.confirmNoPurchase(staff.getId(), Instant.now());
    VisitMemory memory =
        VisitMemory.create(
            visit.getId(), visit.getCustomerId(), staff.getId(), snapshot(), "visit-memory-v1");
    memory.startGeneration();
    memory.completeGeneration("{\"summary\":\"최종 기록\"}", Instant.now());
    when(visitMemoryRepository.findById(memory.getId())).thenReturn(Optional.of(memory));
    when(visitRepository.findByIdAndStoreId(visit.getId(), storeId)).thenReturn(Optional.of(visit));
    when(customerNotificationRepository.existsByCustomerIdAndResourceId(
            visit.getCustomerId(), memory.getId()))
        .thenReturn(false);

    service.share(memory.getId(), staff);

    assertThat(memory.getStatus()).isEqualTo(VisitMemoryStatus.FINALIZED);
    assertThat(visit.getStatus()).isEqualTo(VisitStatus.COMPLETED);
    verify(customerNotificationRepository).saveAndFlush(any(CustomerNotification.class));
  }

  @Test
  void 이미_Visit_Memory가_있는_방문은_중복_생성하지_않는다() {
    Staff staff = staff();
    Visit visit = visit(InteractionStyle.SELF_GUIDED);
    VisitMemory existing =
        VisitMemory.create(
            visit.getId(), visit.getCustomerId(), staff.getId(), snapshot(), "visit-memory-v1");
    when(visitRepository.findByIdAndStoreId(visit.getId(), storeId)).thenReturn(Optional.of(visit));
    when(visitMemoryRepository.findByVisitId(visit.getId())).thenReturn(Optional.of(existing));

    assertThatThrownBy(
            () ->
                service.prepareInitial(
                    visit.getId(), staff, new StaffVisitMemoryGenerationRequest(snapshot())))
        .isInstanceOf(BusinessException.class);
  }

  private Staff staff() {
    return Staff.create(storeId, "김형진", "hashed-token", Set.of(LanguageCode.EN));
  }

  private Customer customer(UUID customerId, String name) {
    Customer customer = Customer.create("customer-token-hash");
    customer.updateName(name);
    return customer;
  }

  private Visit visit(InteractionStyle interactionStyle) {
    Visit visit = Visit.create(UUID.randomUUID(), storeId);
    visit.completeOnboarding(
        com.lionthanflower.domain.common.entity.LanguageCode.EN, interactionStyle, null);
    return visit;
  }

  private VisitMemoryInputSnapshot snapshot() {
    return new VisitMemoryInputSnapshot(
        Map.of(UUID.randomUUID(), Set.of(ProductEngagement.VIEWED_WITH_INTEREST)),
        Set.of(CustomerInterestPoint.DESIGN),
        null,
        Set.of(NoPurchaseReason.NEED_MORE_TIME),
        null,
        "다음 방문에 안내");
  }
}
