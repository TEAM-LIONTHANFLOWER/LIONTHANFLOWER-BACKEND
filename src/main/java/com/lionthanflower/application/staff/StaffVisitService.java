// 직원의 매장에 속한 현재 방문 고객 목록을 조회하는 서비스
package com.lionthanflower.application.staff;

import com.lionthanflower.application.staff.dto.VisitSummaryResponse;
import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import com.lionthanflower.infrastructure.persistence.VisitRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffVisitService {

  private static final Set<VisitStatus> EXCLUDED_STATUSES =
      Set.of(VisitStatus.COMPLETED, VisitStatus.CANCELED);

  private final VisitRepository visitRepository;
  private final CustomerRepository customerRepository;

  public StaffVisitService(VisitRepository visitRepository, CustomerRepository customerRepository) {
    this.visitRepository = visitRepository;
    this.customerRepository = customerRepository;
  }

  @Transactional(readOnly = true)
  public List<VisitSummaryResponse> getCurrentVisits(UUID storeId) {
    List<Visit> visits = visitRepository.findByStoreIdAndStatusNotIn(storeId, EXCLUDED_STATUSES);

    Set<UUID> customerIds = visits.stream().map(Visit::getCustomerId).collect(Collectors.toSet());
    Map<UUID, String> customerNames =
        customerRepository.findAllById(customerIds).stream()
            .collect(Collectors.toMap(Customer::getId, Customer::getName));

    return visits.stream()
        .map(visit -> VisitSummaryResponse.of(visit, customerNames.get(visit.getCustomerId())))
        .toList();
  }
}
