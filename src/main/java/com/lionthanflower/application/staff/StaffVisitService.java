// 직원의 현재 방문 고객 조회와 직원 추천 고객 매칭을 처리하는 서비스
package com.lionthanflower.application.staff;

import com.lionthanflower.application.staff.dto.StaffVisitAssignmentResponse;
import com.lionthanflower.application.staff.dto.VisitSummaryResponse;
import com.lionthanflower.domain.arc.entity.Arc;
import com.lionthanflower.domain.arc.entity.ArcStatus;
import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.visit.entity.InteractionStyle;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import com.lionthanflower.domain.visit.error.VisitErrorCode;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.infrastructure.persistence.ArcRepository;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import com.lionthanflower.infrastructure.persistence.VisitRepository;
import java.time.Instant;
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
  private static final Set<ArcStatus> VISIBLE_ARC_STATUSES =
      Set.of(ArcStatus.SHARED, ArcStatus.FINALIZED);

  private final VisitRepository visitRepository;
  private final CustomerRepository customerRepository;
  private final ArcRepository arcRepository;

  public StaffVisitService(
      VisitRepository visitRepository,
      CustomerRepository customerRepository,
      ArcRepository arcRepository) {
    this.visitRepository = visitRepository;
    this.customerRepository = customerRepository;
    this.arcRepository = arcRepository;
  }

  @Transactional(readOnly = true)
  public List<VisitSummaryResponse> getCurrentVisits(UUID storeId) {
    List<Visit> visits = visitRepository.findByStoreIdAndStatusNotIn(storeId, EXCLUDED_STATUSES);

    Set<UUID> customerIds = visits.stream().map(Visit::getCustomerId).collect(Collectors.toSet());
    Map<UUID, String> customerNames =
        customerRepository.findAllById(customerIds).stream()
            .collect(Collectors.toMap(Customer::getId, Customer::getName));
    Map<UUID, Long> arcCounts =
        customerIds.isEmpty()
            ? Map.of()
            : arcRepository
                .findByCustomerIdInAndStatusIn(customerIds, VISIBLE_ARC_STATUSES)
                .stream()
                .map(Arc::getCustomerId)
                .collect(Collectors.groupingBy(customerId -> customerId, Collectors.counting()));

    return visits.stream()
        .map(
            visit ->
                VisitSummaryResponse.of(
                    visit,
                    customerNames.get(visit.getCustomerId()),
                    arcCounts.getOrDefault(visit.getCustomerId(), 0L)))
        .toList();
  }

  @Transactional
  public StaffVisitAssignmentResponse assignVisit(UUID visitId, Staff staff) {
    Visit visit =
        visitRepository
            .findByIdAndStoreId(visitId, staff.getStoreId())
            .orElseThrow(() -> new BusinessException(VisitErrorCode.NOT_FOUND));

    if (visit.getStatus() != VisitStatus.WAITING_FOR_STAFF
        || visit.getInteractionStyle() != InteractionStyle.STAFF_RECOMMENDATION
        || visit.getStaffId() != null) {
      throw new BusinessException(VisitErrorCode.NOT_ASSIGNABLE);
    }

    visit.assignStaff(staff.getId(), Instant.now());
    return StaffVisitAssignmentResponse.from(visit);
  }
}
