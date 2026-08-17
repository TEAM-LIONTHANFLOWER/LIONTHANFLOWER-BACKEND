// 고객이 보유한 Arc의 공개 범위를 검증하고 목록과 상세 응답을 조합하는 Application Service
package com.lionthanflower.application.customer;

import com.lionthanflower.domain.arc.entity.Arc;
import com.lionthanflower.domain.arc.entity.ArcGeneratedContent;
import com.lionthanflower.domain.arc.entity.ArcInputSnapshot;
import com.lionthanflower.domain.arc.entity.ArcRevision;
import com.lionthanflower.domain.arc.entity.ArcStatus;
import com.lionthanflower.domain.common.entity.SnapshotJsonSerializer;
import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.product.entity.Product;
import com.lionthanflower.domain.product.entity.ProductColor;
import com.lionthanflower.domain.product.entity.ProductOption;
import com.lionthanflower.domain.product.entity.ProductVariant;
import com.lionthanflower.domain.store.entity.Store;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.global.error.BusinessException;
import com.lionthanflower.global.error.CommonErrorCode;
import com.lionthanflower.infrastructure.persistence.ArcRepository;
import com.lionthanflower.infrastructure.persistence.ArcRevisionRepository;
import com.lionthanflower.infrastructure.persistence.CustomerRepository;
import com.lionthanflower.infrastructure.persistence.ProductRepository;
import com.lionthanflower.infrastructure.persistence.ProductVariantRepository;
import com.lionthanflower.infrastructure.persistence.StoreRepository;
import com.lionthanflower.infrastructure.persistence.VisitRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CustomerArcQueryService {

  private static final List<ArcStatus> VISIBLE_STATUSES =
      List.of(ArcStatus.SHARED, ArcStatus.FINALIZED);

  private final CustomerRepository customerRepository;
  private final ArcRepository arcRepository;
  private final ArcRevisionRepository arcRevisionRepository;
  private final VisitRepository visitRepository;
  private final StoreRepository storeRepository;
  private final ProductVariantRepository productVariantRepository;
  private final ProductRepository productRepository;
  private final CustomerTokenManager tokenManager;

  public CustomerArcQueryService(
      CustomerRepository customerRepository,
      ArcRepository arcRepository,
      ArcRevisionRepository arcRevisionRepository,
      VisitRepository visitRepository,
      StoreRepository storeRepository,
      ProductVariantRepository productVariantRepository,
      ProductRepository productRepository,
      CustomerTokenManager tokenManager) {
    this.customerRepository = customerRepository;
    this.arcRepository = arcRepository;
    this.arcRevisionRepository = arcRevisionRepository;
    this.visitRepository = visitRepository;
    this.storeRepository = storeRepository;
    this.productVariantRepository = productVariantRepository;
    this.productRepository = productRepository;
    this.tokenManager = tokenManager;
  }

  public List<ArcSummary> getArcs(String rawToken) {
    Customer customer = requireCustomer(rawToken);
    List<Arc> arcs =
        arcRepository.findByCustomerIdAndStatusInOrderByArcNumberDesc(
            customer.getId(), VISIBLE_STATUSES);
    if (arcs.isEmpty()) {
      return List.of();
    }

    Map<UUID, ArcRevision> revisions = loadActiveRevisions(arcs);
    Map<UUID, ProductVariant> variants = loadVariants(revisions.values());
    Map<UUID, Product> products = loadProducts(variants.values());
    return arcs.stream().map(arc -> toSummary(arc, revisions, variants, products)).toList();
  }

  public ArcDetail getArc(UUID arcId, String rawToken) {
    Customer customer = requireCustomer(rawToken);
    Arc arc =
        arcRepository
            .findByIdAndCustomerIdAndStatusIn(arcId, customer.getId(), VISIBLE_STATUSES)
            .orElseThrow(() -> new BusinessException(CommonErrorCode.NOT_FOUND));
    ArcRevision revision = loadActiveRevision(arc);
    ArcInputSnapshot snapshot = deserialize(revision.getInputSnapshot(), ArcInputSnapshot.class);
    ArcGeneratedContent content =
        deserialize(revision.getGeneratedContent(), ArcGeneratedContent.class);
    Map<UUID, ProductVariant> variants = loadVariants(List.of(revision));
    Map<UUID, Product> products = loadProducts(variants.values());
    List<ProductView> purchasedProducts = toProducts(snapshot, variants, products);
    Visit visit =
        visitRepository
            .findById(arc.getVisitId())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR));
    Store store =
        storeRepository
            .findById(visit.getStoreId())
            .orElseThrow(() -> new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR));
    String customerName = requireText(customer.getName(), "고객 이름");

    return new ArcDetail(
        arc.getId(),
        requireArcNumber(arc),
        customerName,
        store.getName(),
        store.getCountryCode(),
        arc.getStatus(),
        arc.getSharedAt(),
        arc.getFinalizedAt(),
        content.momentSummary(),
        content.preferences(),
        content.momentToRemember(),
        purchasedProducts);
  }

  private ArcSummary toSummary(
      Arc arc,
      Map<UUID, ArcRevision> revisions,
      Map<UUID, ProductVariant> variants,
      Map<UUID, Product> products) {
    ArcRevision revision = requireRevision(revisions.get(activeRevisionId(arc)));
    ArcInputSnapshot snapshot = deserialize(revision.getInputSnapshot(), ArcInputSnapshot.class);
    ArcGeneratedContent content =
        deserialize(revision.getGeneratedContent(), ArcGeneratedContent.class);
    List<ProductView> purchasedProducts = toProducts(snapshot, variants, products);
    if (purchasedProducts.isEmpty()) {
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
    return new ArcSummary(
        arc.getId(),
        requireArcNumber(arc),
        content.momentSummary(),
        purchasedProducts.getFirst(),
        arc.getStatus(),
        arc.getSharedAt(),
        arc.getFinalizedAt());
  }

  private Map<UUID, ArcRevision> loadActiveRevisions(List<Arc> arcs) {
    List<UUID> revisionIds = arcs.stream().map(this::activeRevisionId).toList();
    return arcRevisionRepository.findAllById(revisionIds).stream()
        .collect(Collectors.toMap(ArcRevision::getId, Function.identity()));
  }

  private Map<UUID, ProductVariant> loadVariants(Iterable<ArcRevision> revisions) {
    List<UUID> variantIds =
        java.util.stream.StreamSupport.stream(revisions.spliterator(), false)
            .map(revision -> deserialize(revision.getInputSnapshot(), ArcInputSnapshot.class))
            .flatMap(snapshot -> snapshot.purchasedProductVariantIds().stream())
            .distinct()
            .toList();
    return productVariantRepository.findAllById(variantIds).stream()
        .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
  }

  private Map<UUID, Product> loadProducts(Iterable<ProductVariant> variants) {
    List<UUID> productIds =
        java.util.stream.StreamSupport.stream(variants.spliterator(), false)
            .map(ProductVariant::getProductId)
            .distinct()
            .toList();
    return productRepository.findAllById(productIds).stream()
        .collect(Collectors.toMap(Product::getId, Function.identity()));
  }

  private List<ProductView> toProducts(
      ArcInputSnapshot snapshot, Map<UUID, ProductVariant> variants, Map<UUID, Product> products) {
    return snapshot.purchasedProductVariantIds().stream()
        .map(
            variantId -> {
              ProductVariant variant = requireReference(variants.get(variantId));
              Product product = requireReference(products.get(variant.getProductId()));
              return new ProductView(
                  variant.getId(), product.getName(), variant.getColor(), variant.getOption());
            })
        .toList();
  }

  private ArcRevision loadActiveRevision(Arc arc) {
    UUID revisionId = activeRevisionId(arc);
    return arcRevisionRepository
        .findById(revisionId)
        .map(this::requireRevision)
        .orElseThrow(() -> new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR));
  }

  private UUID activeRevisionId(Arc arc) {
    UUID revisionId =
        arc.getStatus() == ArcStatus.FINALIZED
            ? arc.getFinalRevisionId()
            : arc.getSharedRevisionId();
    if (revisionId == null || arc.getArcNumber() == null) {
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
    return revisionId;
  }

  private Customer requireCustomer(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw new BusinessException(CommonErrorCode.UNAUTHORIZED);
    }
    return customerRepository
        .findByTokenHash(tokenManager.hash(rawToken))
        .orElseThrow(() -> new BusinessException(CommonErrorCode.UNAUTHORIZED));
  }

  private <T> T deserialize(String json, Class<T> type) {
    try {
      return SnapshotJsonSerializer.deserialize(json, type);
    } catch (IllegalArgumentException exception) {
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  private ArcRevision requireRevision(ArcRevision revision) {
    if (revision == null) {
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
    return revision;
  }

  private int requireArcNumber(Arc arc) {
    if (arc.getArcNumber() == null) {
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
    return arc.getArcNumber();
  }

  private <T> T requireReference(T value) {
    if (value == null) {
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
    return value;
  }

  private String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }
    return value.trim();
  }

  public record ProductView(
      UUID productVariantId, String productName, ProductColor color, ProductOption option) {}

  public record ArcSummary(
      UUID arcId,
      int arcNumber,
      String momentSummary,
      ProductView representativeProduct,
      ArcStatus status,
      Instant sharedAt,
      Instant finalizedAt) {}

  public record ArcDetail(
      UUID arcId,
      int arcNumber,
      String customerName,
      String storeName,
      String countryCode,
      ArcStatus status,
      Instant sharedAt,
      Instant finalizedAt,
      String momentSummary,
      List<String> preferences,
      String momentToRemember,
      List<ProductView> purchasedProducts) {}
}
