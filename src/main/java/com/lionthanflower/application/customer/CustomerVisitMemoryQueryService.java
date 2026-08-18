// 고객에게 공개된 Visit Memory 상세와 매장 정보를 조합하는 서비스
package com.lionthanflower.application.customer;

import com.lionthanflower.domain.common.entity.SnapshotJsonSerializer;
import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.store.entity.Store;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visitmemory.entity.VisitMemory;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryGeneratedContent;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryStatus;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.error.CommonErrorCode;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import com.lionthanflower.infrastructure.persistence.StoreRepository;
import com.lionthanflower.infrastructure.persistence.VisitMemoryRepository;
import com.lionthanflower.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerVisitMemoryQueryService {

  private final CustomerRepository customerRepository;
  private final VisitMemoryRepository visitMemoryRepository;
  private final VisitRepository visitRepository;
  private final StoreRepository storeRepository;
  private final CustomerTokenManager tokenManager;

  public CustomerVisitMemoryQueryService(
      CustomerRepository customerRepository,
      VisitMemoryRepository visitMemoryRepository,
      VisitRepository visitRepository,
      StoreRepository storeRepository,
      CustomerTokenManager tokenManager) {
    this.customerRepository = customerRepository;
    this.visitMemoryRepository = visitMemoryRepository;
    this.visitRepository = visitRepository;
    this.storeRepository = storeRepository;
    this.tokenManager = tokenManager;
  }

  public VisitMemoryDetail getMemory(UUID visitMemoryId, String rawToken) {
    Customer customer = requireCustomer(rawToken);
    VisitMemory memory =
        visitMemoryRepository
            .findByIdAndCustomerId(visitMemoryId, customer.getId())
            .filter(value -> value.getStatus() == VisitMemoryStatus.FINALIZED)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
    Visit visit =
        visitRepository
            .findById(memory.getVisitId())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
    Store store =
        storeRepository
            .findById(visit.getStoreId())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
    VisitMemoryGeneratedContent content = deserialize(memory.getGeneratedContent());
    return new VisitMemoryDetail(
        memory.getId(),
        memory.getVisitId(),
        store.getName(),
        store.getCountryCode(),
        content.summary(),
        memory.getFinalizedAt());
  }

  private Customer requireCustomer(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
    }
    return customerRepository
        .findByTokenHash(tokenManager.hash(rawToken))
        .orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));
  }

  private VisitMemoryGeneratedContent deserialize(String generatedContent) {
    try {
      return SnapshotJsonSerializer.deserialize(
          generatedContent, VisitMemoryGeneratedContent.class);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  public record VisitMemoryDetail(
      UUID visitMemoryId,
      UUID visitId,
      String storeName,
      String countryCode,
      String summary,
      Instant finalizedAt) {}
}
