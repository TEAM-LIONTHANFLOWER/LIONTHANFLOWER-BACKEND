// Arc와 리비전의 생성·실패·공유 상태를 트랜잭션 단위로 관리하는 서비스
package com.lionthanflower.application.staff;

import com.lionthanflower.application.arc.ArcGenerationCommand;
import com.lionthanflower.application.staff.dto.StaffArcGenerationRequest;
import com.lionthanflower.application.staff.dto.StaffArcRevisionResponse;
import com.lionthanflower.domain.arc.entity.Arc;
import com.lionthanflower.domain.arc.entity.ArcGeneratedContent;
import com.lionthanflower.domain.arc.entity.ArcInputSnapshot;
import com.lionthanflower.domain.arc.entity.ArcRevision;
import com.lionthanflower.domain.arc.entity.ArcRevisionStatus;
import com.lionthanflower.domain.arc.entity.ArcStatus;
import com.lionthanflower.domain.arc.error.ArcErrorCode;
import com.lionthanflower.domain.common.entity.SnapshotJsonSerializer;
import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.purchase.entity.Purchase;
import com.lionthanflower.domain.purchase.entity.PurchaseItem;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.error.CommonErrorCode;
import com.lionthanflower.infrastructure.persistence.ArcRepository;
import com.lionthanflower.infrastructure.persistence.ArcRevisionRepository;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import com.lionthanflower.infrastructure.persistence.PurchaseItemRepository;
import com.lionthanflower.infrastructure.persistence.PurchaseRepository;
import com.lionthanflower.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StaffArcStateService {

  private static final Set<ArcStatus> VISIBLE_STATUSES =
      Set.of(ArcStatus.SHARED, ArcStatus.FINALIZED);
  private static final Set<String> ARC_UNIQUE_CONSTRAINTS =
      Set.of(
          "uk_purchases_visit_id",
          "uk_purchase_items_purchase_variant",
          "uk_arcs_visit_id",
          "uk_arcs_purchase_id",
          "uk_arcs_customer_arc_number",
          "uk_arc_revisions_arc_number");

  private final VisitRepository visitRepository;
  private final CustomerRepository customerRepository;
  private final PurchaseRepository purchaseRepository;
  private final PurchaseItemRepository purchaseItemRepository;
  private final ArcRepository arcRepository;
  private final ArcRevisionRepository arcRevisionRepository;
  private final String templateVersion;

  public StaffArcStateService(
      VisitRepository visitRepository,
      CustomerRepository customerRepository,
      PurchaseRepository purchaseRepository,
      PurchaseItemRepository purchaseItemRepository,
      ArcRepository arcRepository,
      ArcRevisionRepository arcRevisionRepository,
      @Value("${app.arc.template-version:arc-v1}") String templateVersion) {
    this.visitRepository = visitRepository;
    this.customerRepository = customerRepository;
    this.purchaseRepository = purchaseRepository;
    this.purchaseItemRepository = purchaseItemRepository;
    this.arcRepository = arcRepository;
    this.arcRevisionRepository = arcRevisionRepository;
    this.templateVersion = templateVersion;
  }

  @Transactional
  public GenerationContext prepareInitial(
      UUID visitId, Staff staff, StaffArcGenerationRequest request) {
    Visit visit = findVisit(visitId, staff);
    if (visit.getStatus() != VisitStatus.ACTIVE) {
      throw new BusinessException(ArcErrorCode.NOT_ASSIGNABLE);
    }
    assignIfNeeded(visit, staff);
    if (arcRepository.findByVisitId(visitId).isPresent()) {
      throw new BusinessException(ArcErrorCode.ALREADY_EXISTS);
    }

    ArcInputSnapshot inputSnapshot = requireInput(request);
    visit.confirmPurchase(staff.getId(), Instant.now());
    Customer customer = findCustomer(visit.getCustomerId());
    try {
      Purchase purchase = purchaseRepository.saveAndFlush(Purchase.create(visit.getId()));
      purchaseItemRepository.saveAll(
          PurchaseItem.createAll(purchase.getId(), inputSnapshot.purchasedProductVariantIds()));
      Arc arc =
          arcRepository.saveAndFlush(
              Arc.create(visit.getId(), purchase.getId(), customer.getId(), staff.getId()));
      ArcRevision revision =
          arcRevisionRepository.save(
              ArcRevision.start(arc.getId(), 1, inputSnapshot, templateVersion, staff.getId()));
      return context(arc, revision, customer, visit, inputSnapshot);
    } catch (DataIntegrityViolationException exception) {
      throw translateUniqueConstraint(exception);
    }
  }

  @Transactional
  public GenerationContext prepareRevision(
      UUID arcId, Staff staff, StaffArcGenerationRequest request) {
    Arc arc =
        arcRepository
            .findById(arcId)
            .orElseThrow(() -> new BusinessException(ArcErrorCode.NOT_FOUND));
    Visit visit = findVisit(arc.getVisitId(), staff);
    if (arc.getStatus() != ArcStatus.DRAFT) {
      throw new BusinessException(ArcErrorCode.NOT_ASSIGNABLE);
    }
    verifyAssignedStaff(visit, staff);
    ArcRevision previous =
        arcRevisionRepository
            .findTopByArcIdOrderByRevisionNumberDesc(arcId)
            .orElseThrow(() -> new BusinessException(ArcErrorCode.NOT_FOUND));
    ArcInputSnapshot inputSnapshot =
        request == null || request.inputSnapshot() == null
            ? deserializeInput(previous.getInputSnapshot())
            : requireInput(request);
    Purchase purchase =
        purchaseRepository
            .findByVisitId(visit.getId())
            .orElseThrow(() -> new BusinessException(ArcErrorCode.NOT_FOUND));
    purchaseItemRepository.deleteByPurchaseId(purchase.getId());
    purchaseItemRepository.saveAll(
        PurchaseItem.createAll(purchase.getId(), inputSnapshot.purchasedProductVariantIds()));
    Customer customer = findCustomer(visit.getCustomerId());
    ArcRevision revision =
        arcRevisionRepository.save(
            ArcRevision.start(
                arc.getId(),
                previous.getRevisionNumber() + 1,
                inputSnapshot,
                templateVersion,
                staff.getId()));
    return context(arc, revision, customer, visit, inputSnapshot);
  }

  @Transactional
  public StaffArcRevisionResponse complete(UUID revisionId, ArcGeneratedContent content) {
    ArcRevision revision = findRevision(revisionId);
    revision.complete(SnapshotJsonSerializer.serialize(content), Instant.now());
    Arc arc = findArc(revision.getArcId());
    return toResponse(arc, revision);
  }

  @Transactional
  public StaffArcRevisionResponse fail(UUID revisionId, String failureCode) {
    ArcRevision revision = findRevision(revisionId);
    revision.fail(failureCode);
    Arc arc = findArc(revision.getArcId());
    return toResponse(arc, revision);
  }

  @Transactional(readOnly = true)
  public StaffArcRevisionResponse getPreview(UUID arcId, Staff staff) {
    Arc arc = findArc(arcId);
    Visit visit = findVisit(arc.getVisitId(), staff);
    verifyAssignedStaff(visit, staff);
    ArcRevision revision =
        arcRevisionRepository
            .findTopByArcIdOrderByRevisionNumberDesc(arcId)
            .orElseThrow(() -> new BusinessException(ArcErrorCode.NOT_FOUND));
    return toResponse(arc, revision);
  }

  @Transactional
  public StaffArcRevisionResponse share(UUID arcId, UUID revisionId, Staff staff) {
    Arc arc = findArc(arcId);
    Visit visit = findVisit(arc.getVisitId(), staff);
    verifyAssignedStaff(visit, staff);
    if (arc.getStatus() != ArcStatus.DRAFT) {
      throw new BusinessException(ArcErrorCode.NOT_ASSIGNABLE);
    }
    ArcRevision revision =
        arcRevisionRepository
            .findByIdAndArcId(revisionId, arcId)
            .orElseThrow(() -> new BusinessException(ArcErrorCode.NOT_FOUND));
    if (revision.getStatus() != ArcRevisionStatus.READY) {
      throw new BusinessException(ArcErrorCode.REVISION_NOT_READY);
    }
    long nextArcNumber =
        arcRepository.countByCustomerIdAndStatusIn(arc.getCustomerId(), VISIBLE_STATUSES) + 1;
    try {
      arc.shareFirst(revision, Instant.now(), Math.toIntExact(nextArcNumber));
      arcRepository.saveAndFlush(arc);
      return toResponse(arc, revision);
    } catch (DataIntegrityViolationException exception) {
      throw translateUniqueConstraint(exception);
    }
  }

  private RuntimeException translateUniqueConstraint(DataIntegrityViolationException exception) {
    if (isArcUniqueConstraintViolation(exception)) {
      return new BusinessException(ArcErrorCode.ALREADY_EXISTS);
    }
    return exception;
  }

  private boolean isArcUniqueConstraintViolation(DataIntegrityViolationException exception) {
    Throwable cause = exception;
    while (cause != null) {
      if (cause instanceof ConstraintViolationException constraintViolation
          && ARC_UNIQUE_CONSTRAINTS.contains(constraintViolation.getConstraintName())) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  private GenerationContext context(
      Arc arc,
      ArcRevision revision,
      Customer customer,
      Visit visit,
      ArcInputSnapshot inputSnapshot) {
    return new GenerationContext(
        arc.getId(),
        revision.getId(),
        new ArcGenerationCommand(customer.getName(), visit.getAdditionalRequest(), inputSnapshot));
  }

  private Visit findVisit(UUID visitId, Staff staff) {
    return visitRepository
        .findByIdAndStoreId(visitId, staff.getStoreId())
        .orElseThrow(() -> new BusinessException(ArcErrorCode.NOT_FOUND));
  }

  private Arc findArc(UUID arcId) {
    return arcRepository
        .findById(arcId)
        .orElseThrow(() -> new BusinessException(ArcErrorCode.NOT_FOUND));
  }

  private ArcRevision findRevision(UUID revisionId) {
    return arcRevisionRepository
        .findById(revisionId)
        .orElseThrow(() -> new BusinessException(ArcErrorCode.NOT_FOUND));
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
      throw new BusinessException(ArcErrorCode.NOT_ASSIGNABLE);
    }
  }

  private ArcInputSnapshot requireInput(StaffArcGenerationRequest request) {
    if (request == null || request.inputSnapshot() == null) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
    }
    try {
      request.inputSnapshot().validatePurchaseInfo();
      return request.inputSnapshot();
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private ArcInputSnapshot deserializeInput(String inputSnapshot) {
    try {
      return SnapshotJsonSerializer.deserialize(inputSnapshot, ArcInputSnapshot.class);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  private StaffArcRevisionResponse toResponse(Arc arc, ArcRevision revision) {
    ArcInputSnapshot inputSnapshot = deserializeInput(revision.getInputSnapshot());
    ArcGeneratedContent generatedContent =
        revision.getGeneratedContent() == null
            ? null
            : deserializeGeneratedContent(revision.getGeneratedContent());
    return new StaffArcRevisionResponse(
        arc.getId(),
        revision.getId(),
        revision.getRevisionNumber(),
        arc.getStatus(),
        revision.getStatus(),
        inputSnapshot,
        generatedContent,
        revision.getFailureCode(),
        revision.getSharedAt());
  }

  private ArcGeneratedContent deserializeGeneratedContent(String generatedContent) {
    try {
      return SnapshotJsonSerializer.deserialize(generatedContent, ArcGeneratedContent.class);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  public record GenerationContext(
      UUID arcId, UUID revisionId, ArcGenerationCommand generationCommand) {}
}
