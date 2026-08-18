// 직원 Visit Memory의 상태 전이와 방문·고객 알림 트랜잭션을 관리하는 서비스
package com.lionthanflower.application.staff;

import com.lionthanflower.application.staff.dto.StaffVisitMemoryGenerationRequest;
import com.lionthanflower.application.staff.dto.StaffVisitMemoryResponse;
import com.lionthanflower.application.visitmemory.VisitMemoryGenerationCommand;
import com.lionthanflower.domain.common.entity.SnapshotJsonSerializer;
import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.notification.entity.CustomerNotification;
import com.lionthanflower.domain.notification.entity.CustomerNotificationType;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import com.lionthanflower.domain.visit.error.VisitErrorCode;
import com.lionthanflower.domain.visitmemory.entity.VisitMemory;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryGeneratedContent;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryInputSnapshot;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryStatus;
import com.lionthanflower.domain.visitmemory.error.VisitMemoryErrorCode;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.error.CommonErrorCode;
import com.lionthanflower.infrastructure.persistence.CustomerNotificationRepository;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import com.lionthanflower.infrastructure.persistence.ProductVariantRepository;
import com.lionthanflower.infrastructure.persistence.VisitMemoryRepository;
import com.lionthanflower.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffVisitMemoryStateService {

  private static final String NOTIFICATION_MESSAGE = "새로운 Visit Memory가 도착했습니다.";
  private static final Set<String> UNIQUE_CONSTRAINTS =
      Set.of("uk_visit_memories_visit_id", "uk_customer_notifications_type_resource");

  private final VisitRepository visitRepository;
  private final CustomerRepository customerRepository;
  private final ProductVariantRepository productVariantRepository;
  private final VisitMemoryRepository visitMemoryRepository;
  private final CustomerNotificationRepository customerNotificationRepository;
  private final String templateVersion;

  public StaffVisitMemoryStateService(
      VisitRepository visitRepository,
      CustomerRepository customerRepository,
      ProductVariantRepository productVariantRepository,
      VisitMemoryRepository visitMemoryRepository,
      CustomerNotificationRepository customerNotificationRepository,
      @Value("${app.visit-memory.template-version:visit-memory-v1}") String templateVersion) {
    this.visitRepository = visitRepository;
    this.customerRepository = customerRepository;
    this.productVariantRepository = productVariantRepository;
    this.visitMemoryRepository = visitMemoryRepository;
    this.customerNotificationRepository = customerNotificationRepository;
    this.templateVersion = templateVersion;
  }

  @Transactional
  public GenerationContext prepareInitial(
      UUID visitId, Staff staff, StaffVisitMemoryGenerationRequest request) {
    Visit visit = findVisit(visitId, staff);
    if (visit.getStatus() != VisitStatus.ACTIVE) {
      throw new BusinessException(VisitMemoryErrorCode.NOT_ASSIGNABLE);
    }
    assignIfNeeded(visit, staff);
    if (visitMemoryRepository.findByVisitId(visitId).isPresent()) {
      throw new BusinessException(VisitMemoryErrorCode.ALREADY_EXISTS);
    }
    VisitMemoryInputSnapshot inputSnapshot = requireInput(request);
    validateProductVariants(inputSnapshot);
    visit.confirmNoPurchase(staff.getId(), Instant.now());
    Customer customer = findCustomer(visit.getCustomerId());
    VisitMemory memory;
    try {
      memory =
          visitMemoryRepository.saveAndFlush(
              VisitMemory.create(
                  visit.getId(), customer.getId(), staff.getId(), inputSnapshot, templateVersion));
    } catch (DataIntegrityViolationException exception) {
      throw translateUniqueConstraint(exception);
    }
    memory.startGeneration();
    return context(memory, customer, visit, inputSnapshot);
  }

  @Transactional
  public GenerationContext prepareRegeneration(
      UUID visitMemoryId, Staff staff, StaffVisitMemoryGenerationRequest request) {
    VisitMemory memory = findMemory(visitMemoryId);
    Visit visit = findVisit(memory.getVisitId(), staff);
    verifyAssignedStaff(visit, staff);
    if (visit.getStatus() != VisitStatus.VISIT_MEMORY_IN_PROGRESS) {
      throw new BusinessException(VisitMemoryErrorCode.NOT_ASSIGNABLE);
    }
    if (memory.getStatus() != VisitMemoryStatus.READY
        && memory.getStatus() != VisitMemoryStatus.FAILED) {
      throw new BusinessException(VisitMemoryErrorCode.NOT_ASSIGNABLE);
    }
    VisitMemoryInputSnapshot inputSnapshot =
        request == null || request.inputSnapshot() == null
            ? deserializeInput(memory.getInputSnapshot())
            : requireInput(request);
    validateProductVariants(inputSnapshot);
    Customer customer = findCustomer(visit.getCustomerId());
    memory.replaceInput(inputSnapshot);
    memory.startGeneration();
    return context(memory, customer, visit, inputSnapshot);
  }

  @Transactional
  public StaffVisitMemoryResponse complete(
      UUID visitMemoryId, VisitMemoryGeneratedContent generatedContent) {
    VisitMemory memory = findMemory(visitMemoryId);
    memory.completeGeneration(SnapshotJsonSerializer.serialize(generatedContent), Instant.now());
    return toResponse(memory);
  }

  @Transactional
  public StaffVisitMemoryResponse fail(UUID visitMemoryId, String failureCode) {
    VisitMemory memory = findMemory(visitMemoryId);
    memory.fail(failureCode);
    return toResponse(memory);
  }

  @Transactional(readOnly = true)
  public StaffVisitMemoryResponse getPreview(UUID visitMemoryId, Staff staff) {
    VisitMemory memory = findMemory(visitMemoryId);
    Visit visit = findVisit(memory.getVisitId(), staff);
    verifyAssignedStaff(visit, staff);
    return toResponse(memory);
  }

  @Transactional
  public StaffVisitMemoryResponse share(UUID visitMemoryId, Staff staff) {
    VisitMemory memory = findMemory(visitMemoryId);
    Visit visit = findVisit(memory.getVisitId(), staff);
    verifyAssignedStaff(visit, staff);
    if (memory.getStatus() != VisitMemoryStatus.READY) {
      throw new BusinessException(VisitMemoryErrorCode.NOT_READY);
    }
    Instant now = Instant.now();
    memory.finalizeMemory(now);
    visit.complete(now);
    if (!customerNotificationRepository.existsByCustomerIdAndTypeAndResourceId(
        memory.getCustomerId(), CustomerNotificationType.VISIT_MEMORY, memory.getId())) {
      try {
        customerNotificationRepository.saveAndFlush(
            CustomerNotification.createVisitMemory(
                memory.getCustomerId(), memory.getId(), NOTIFICATION_MESSAGE));
      } catch (DataIntegrityViolationException exception) {
        throw translateUniqueConstraint(exception);
      }
    }
    return toResponse(memory);
  }

  private GenerationContext context(
      VisitMemory memory, Customer customer, Visit visit, VisitMemoryInputSnapshot inputSnapshot) {
    return new GenerationContext(
        memory.getId(),
        new VisitMemoryGenerationCommand(
            customer.getName(), visit.getAdditionalRequest(), inputSnapshot));
  }

  private Visit findVisit(UUID visitId, Staff staff) {
    return visitRepository
        .findByIdAndStoreId(visitId, staff.getStoreId())
        .orElseThrow(() -> new BusinessException(VisitErrorCode.NOT_FOUND));
  }

  private VisitMemory findMemory(UUID visitMemoryId) {
    return visitMemoryRepository
        .findById(visitMemoryId)
        .orElseThrow(() -> new BusinessException(VisitMemoryErrorCode.NOT_FOUND));
  }

  private Customer findCustomer(UUID customerId) {
    return customerRepository
        .findById(customerId)
        .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
  }

  private void assignIfNeeded(Visit visit, Staff staff) {
    if (visit.getStaffId() == null) {
      visit.assignStaff(staff.getId(), Instant.now());
      return;
    }
    verifyAssignedStaff(visit, staff);
  }

  private void verifyAssignedStaff(Visit visit, Staff staff) {
    if (!staff.getId().equals(visit.getStaffId())) {
      throw new BusinessException(VisitMemoryErrorCode.NOT_ASSIGNABLE);
    }
  }

  private VisitMemoryInputSnapshot requireInput(StaffVisitMemoryGenerationRequest request) {
    if (request == null || request.inputSnapshot() == null) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
    }
    return request.inputSnapshot();
  }

  private void validateProductVariants(VisitMemoryInputSnapshot inputSnapshot) {
    List<UUID> ids = inputSnapshot.productEngagements().keySet().stream().toList();
    if (ids.isEmpty()) {
      return;
    }
    if (productVariantRepository.findAllById(ids).size() != ids.size()) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private RuntimeException translateUniqueConstraint(DataIntegrityViolationException exception) {
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof ConstraintViolationException constraintViolation
          && UNIQUE_CONSTRAINTS.contains(constraintViolation.getConstraintName())) {
        return new BusinessException(
            constraintViolation.getConstraintName().equals("uk_visit_memories_visit_id")
                ? VisitMemoryErrorCode.ALREADY_EXISTS
                : VisitMemoryErrorCode.NOT_ASSIGNABLE);
      }
      cause = cause.getCause();
    }
    return exception;
  }

  private VisitMemoryInputSnapshot deserializeInput(String inputSnapshot) {
    try {
      return SnapshotJsonSerializer.deserialize(inputSnapshot, VisitMemoryInputSnapshot.class);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  private StaffVisitMemoryResponse toResponse(VisitMemory memory) {
    VisitMemoryInputSnapshot inputSnapshot = deserializeInput(memory.getInputSnapshot());
    VisitMemoryGeneratedContent generatedContent =
        memory.getGeneratedContent() == null
            ? null
            : deserializeGeneratedContent(memory.getGeneratedContent());
    return new StaffVisitMemoryResponse(
        memory.getId(),
        memory.getVisitId(),
        memory.getStatus(),
        inputSnapshot,
        generatedContent,
        memory.getFailureCode(),
        memory.getGeneratedAt(),
        memory.getFinalizedAt());
  }

  private VisitMemoryGeneratedContent deserializeGeneratedContent(String generatedContent) {
    try {
      return SnapshotJsonSerializer.deserialize(
          generatedContent, VisitMemoryGeneratedContent.class);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  public record GenerationContext(
      UUID visitMemoryId, VisitMemoryGenerationCommand generationCommand) {}
}
