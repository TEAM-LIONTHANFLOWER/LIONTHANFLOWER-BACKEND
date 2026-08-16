// 고객 서비스 진입과 방문 생성을 조정하는 Application Service
package com.lionthanflower.application.customer;

import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.store.entity.Store;
import com.lionthanflower.domain.visit.entity.InteractionStyle;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.error.CommonErrorCode;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import com.lionthanflower.infrastructure.persistence.StoreRepository;
import com.lionthanflower.infrastructure.persistence.VisitRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerVisitService {

  private final CustomerRepository customerRepository;
  private final StoreRepository storeRepository;
  private final VisitRepository visitRepository;
  private final CustomerTokenManager tokenManager;
  private final String storeCode;

  public CustomerVisitService(
      CustomerRepository customerRepository,
      StoreRepository storeRepository,
      VisitRepository visitRepository,
      CustomerTokenManager tokenManager,
      @Value("${app.onboarding.store-code:MCM-SEOUL}") String storeCode) {
    this.customerRepository = customerRepository;
    this.storeRepository = storeRepository;
    this.visitRepository = visitRepository;
    this.tokenManager = tokenManager;
    this.storeCode = storeCode;
  }

  public EntryResult enter(String rawToken) {
    Store store =
        storeRepository
            .findByCode(storeCode)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR));
    CustomerSession session = resolveCustomer(rawToken);
    Visit visit = visitRepository.save(Visit.create(session.customer().getId(), store.getId()));
    return new EntryResult(
        visit.getId(), session.customer().getName(), visit.getStatus(), session.issuedToken());
  }

  public OnboardingResult progressOnboarding(
      UUID visitId, String rawToken, OnboardingCommand command) {
    if (rawToken == null || rawToken.isBlank()) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
    }

    Customer customer =
        customerRepository
            .findByTokenHash(tokenManager.hash(rawToken))
            .orElseThrow(() -> new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE));
    Visit visit =
        visitRepository
            .findById(visitId)
            .filter(found -> found.getCustomerId().equals(customer.getId()))
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
    if (visit.getStatus() != VisitStatus.ONBOARDING) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
    }

    customer.updateName(command.name());
    visit.completeOnboarding(
        command.serviceLanguage(), command.interactionStyle(), command.additionalRequest());
    return new OnboardingResult(visit.getId(), visit.getStatus());
  }

  private CustomerSession resolveCustomer(String rawToken) {
    if (rawToken != null && !rawToken.isBlank()) {
      String tokenHash = tokenManager.hash(rawToken);
      Customer existingCustomer = customerRepository.findByTokenHash(tokenHash).orElse(null);
      if (existingCustomer != null) {
        return new CustomerSession(existingCustomer, null);
      }
    }

    String issuedToken = tokenManager.generate();
    Customer customer = customerRepository.save(Customer.create(tokenManager.hash(issuedToken)));
    return new CustomerSession(customer, issuedToken);
  }

  private record CustomerSession(Customer customer, String issuedToken) {}

  public record EntryResult(
      UUID visitId, String customerName, VisitStatus status, String issuedToken) {}

  public record OnboardingCommand(
      String name,
      LanguageCode serviceLanguage,
      InteractionStyle interactionStyle,
      String additionalRequest) {}

  public record OnboardingResult(UUID visitId, VisitStatus status) {}
}
