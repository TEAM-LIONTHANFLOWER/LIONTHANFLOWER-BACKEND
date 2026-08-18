// 고객이 공유된 Arc를 최종 저장하고 방문을 종료하도록 조정하는 Application Service
package com.lionthanflower.application.customer;

import com.lionthanflower.domain.arc.entity.Arc;
import com.lionthanflower.domain.arc.entity.ArcStatus;
import com.lionthanflower.domain.arc.error.ArcErrorCode;
import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.error.CommonErrorCode;
import com.lionthanflower.infrastructure.persistence.ArcRepository;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import com.lionthanflower.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerArcCommandService {

  private final CustomerRepository customerRepository;
  private final ArcRepository arcRepository;
  private final VisitRepository visitRepository;
  private final CustomerTokenManager tokenManager;

  public CustomerArcCommandService(
      CustomerRepository customerRepository,
      ArcRepository arcRepository,
      VisitRepository visitRepository,
      CustomerTokenManager tokenManager) {
    this.customerRepository = customerRepository;
    this.arcRepository = arcRepository;
    this.visitRepository = visitRepository;
    this.tokenManager = tokenManager;
  }

  public ArcFinalization finalizeArc(UUID arcId, String rawToken) {
    Customer customer = requireCustomer(rawToken);
    Arc arc =
        arcRepository
            .findById(arcId)
            .filter(found -> found.getCustomerId().equals(customer.getId()))
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));

    if (arc.getStatus() == ArcStatus.FINALIZED) {
      return toResult(arc);
    }
    if (arc.getStatus() != ArcStatus.SHARED) {
      throw new BusinessException(ArcErrorCode.NOT_ASSIGNABLE);
    }

    Visit visit =
        visitRepository
            .findById(arc.getVisitId())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR));
    Instant now = Instant.now();
    try {
      arc.finalizeSharedRevision(now);
      visit.complete(now);
    } catch (IllegalStateException exception) {
      throw new BusinessException(ArcErrorCode.NOT_ASSIGNABLE);
    }
    return toResult(arc);
  }

  private Customer requireCustomer(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
    }
    return customerRepository
        .findByTokenHash(tokenManager.hash(rawToken))
        .orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));
  }

  private ArcFinalization toResult(Arc arc) {
    return new ArcFinalization(arc.getId(), arc.getStatus(), arc.getFinalizedAt());
  }

  public record ArcFinalization(UUID arcId, ArcStatus status, Instant finalizedAt) {}
}
