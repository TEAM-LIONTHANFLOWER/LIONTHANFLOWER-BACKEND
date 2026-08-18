// 고객에게 공개된 Visit Memory 상세 조회를 검증하는 테스트
package com.lionthanflower.application.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.store.entity.Store;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visitmemory.entity.VisitMemory;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryInputSnapshot;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import com.lionthanflower.infrastructure.persistence.StoreRepository;
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
class CustomerVisitMemoryQueryServiceTest {

  @Mock private CustomerRepository customerRepository;
  @Mock private VisitMemoryRepository visitMemoryRepository;
  @Mock private VisitRepository visitRepository;
  @Mock private StoreRepository storeRepository;
  @Mock private CustomerTokenManager tokenManager;

  private CustomerVisitMemoryQueryService service;

  @BeforeEach
  void setUp() {
    service =
        new CustomerVisitMemoryQueryService(
            customerRepository,
            visitMemoryRepository,
            visitRepository,
            storeRepository,
            tokenManager);
  }

  @Test
  void 고객_본인의_최종_Visit_Memory_상세를_조회한다() {
    Customer customer = Customer.create("hashed-token");
    Visit visit = Visit.create(customer.getId(), UUID.randomUUID());
    Store store = Store.create("MCM HAUS", "mcm-haus", "KR");
    VisitMemory memory =
        VisitMemory.create(
            visit.getId(), customer.getId(), UUID.randomUUID(), snapshot(), "visit-memory-v1");
    memory.startGeneration();
    memory.completeGeneration("{\"summary\":\"다음 방문을 준비한 기록\"}", Instant.now());
    memory.finalizeMemory(Instant.now());
    when(tokenManager.hash("raw-token")).thenReturn("hashed-token");
    when(customerRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(customer));
    when(visitMemoryRepository.findByIdAndCustomerId(memory.getId(), customer.getId()))
        .thenReturn(Optional.of(memory));
    when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
    when(storeRepository.findById(visit.getStoreId())).thenReturn(Optional.of(store));

    var result = service.getMemory(memory.getId(), "raw-token");

    assertThat(result.summary()).isEqualTo("다음 방문을 준비한 기록");
    assertThat(result.storeName()).isEqualTo("MCM HAUS");
    assertThat(result.countryCode()).isEqualTo("KR");
  }

  private VisitMemoryInputSnapshot snapshot() {
    return new VisitMemoryInputSnapshot(Map.of(), Set.of(), null, Set.of(), null, null);
  }
}
