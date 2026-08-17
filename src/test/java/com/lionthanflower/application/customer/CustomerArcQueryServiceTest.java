// 고객 Arc 목록과 상세 조회 Application Service를 검증하는 단위 테스트
package com.lionthanflower.application.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.lionthanflower.domain.arc.entity.ActualInteractionPreference;
import com.lionthanflower.domain.arc.entity.Arc;
import com.lionthanflower.domain.arc.entity.ArcInputSnapshot;
import com.lionthanflower.domain.arc.entity.ArcRevision;
import com.lionthanflower.domain.arc.entity.ArcStatus;
import com.lionthanflower.domain.arc.entity.PreferredColor;
import com.lionthanflower.domain.arc.entity.PreferredStyle;
import com.lionthanflower.domain.arc.entity.ProductExplanationPreference;
import com.lionthanflower.domain.arc.entity.PurchaseCriterion;
import com.lionthanflower.domain.arc.entity.PurchaseDecisionStyle;
import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.product.entity.Product;
import com.lionthanflower.domain.product.entity.ProductCategory;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerArcQueryServiceTest {

  @Mock private CustomerRepository customerRepository;
  @Mock private ArcRepository arcRepository;
  @Mock private ArcRevisionRepository arcRevisionRepository;
  @Mock private VisitRepository visitRepository;
  @Mock private StoreRepository storeRepository;
  @Mock private ProductVariantRepository productVariantRepository;
  @Mock private ProductRepository productRepository;

  private CustomerTokenManager tokenManager;
  private CustomerArcQueryService service;

  @BeforeEach
  void setUp() {
    tokenManager = new CustomerTokenManager();
    service =
        new CustomerArcQueryService(
            customerRepository,
            arcRepository,
            arcRevisionRepository,
            visitRepository,
            storeRepository,
            productVariantRepository,
            productRepository,
            tokenManager);
  }

  @Test
  void 고객의_공개_Arc_목록을_순번_내림차순과_대표_제품으로_조합한다() {
    String rawToken = "known-token";
    Customer customer = Customer.create(tokenManager.hash(rawToken));
    UUID staffId = UUID.randomUUID();
    Product product = Product.create("BAG-001", "A Bag", ProductCategory.BAG);
    ProductVariant variant =
        ProductVariant.create(
            product.getId(), "BAG-001-BLK-S", ProductColor.BLACK, ProductOption.S);
    Arc arc = Arc.create(UUID.randomUUID(), UUID.randomUUID(), customer.getId(), staffId);
    ArcRevision revision =
        ArcRevision.start(arc.getId(), 1, snapshot(variant.getId()), "arc-v1", staffId);
    revision.complete(
        """
        {"momentSummary":"균형을 중요하게 생각합니다.","preferences":["실용적인 디자인"],"momentToRemember":"수납공간을 오래 고민했습니다."}
        """,
        java.time.Instant.parse("2026-08-15T12:00:00Z"));
    arc.shareFirst(revision, java.time.Instant.parse("2026-08-15T12:01:00Z"), 2);

    when(customerRepository.findByTokenHash(tokenManager.hash(rawToken)))
        .thenReturn(Optional.of(customer));
    when(arcRepository.findByCustomerIdAndStatusInOrderByArcNumberDesc(
            customer.getId(), List.of(ArcStatus.SHARED, ArcStatus.FINALIZED)))
        .thenReturn(List.of(arc));
    when(arcRevisionRepository.findAllById(any())).thenReturn(List.of(revision));
    when(productVariantRepository.findAllById(any())).thenReturn(List.of(variant));
    when(productRepository.findAllById(any())).thenReturn(List.of(product));

    List<CustomerArcQueryService.ArcSummary> result = service.getArcs(rawToken);

    assertThat(result).extracting(CustomerArcQueryService.ArcSummary::arcNumber).containsExactly(2);
    assertThat(result.getFirst().momentSummary()).isEqualTo("균형을 중요하게 생각합니다.");
    assertThat(result.getFirst().representativeProduct().productName()).isEqualTo("A Bag");
  }

  @Test
  void 고객_Arc가_없으면_빈_목록을_반환한다() {
    String rawToken = "known-token";
    Customer customer = Customer.create(tokenManager.hash(rawToken));
    when(customerRepository.findByTokenHash(tokenManager.hash(rawToken)))
        .thenReturn(Optional.of(customer));
    when(arcRepository.findByCustomerIdAndStatusInOrderByArcNumberDesc(
            customer.getId(), List.of(ArcStatus.SHARED, ArcStatus.FINALIZED)))
        .thenReturn(List.of());

    assertThat(service.getArcs(rawToken)).isEmpty();
  }

  @Test
  void 고객_토큰이_없으면_인증_오류를_반환한다() {
    assertThatThrownBy(() -> service.getArcs(null))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(CommonErrorCode.UNAUTHORIZED);
  }

  @Test
  void 최종_저장된_Arc의_상세는_최종_리비전과_전체_구매_제품을_반환한다() {
    String rawToken = "known-token";
    Customer customer = Customer.create(tokenManager.hash(rawToken));
    Store store = Store.create("MCM HAUS", "MCM-SEOUL", "KR");
    Visit visit = Visit.create(customer.getId(), store.getId());
    UUID staffId = UUID.randomUUID();
    Product product = Product.create("BAG-001", "A Bag", ProductCategory.BAG);
    ProductVariant variant =
        ProductVariant.create(
            product.getId(), "BAG-001-BLK-S", ProductColor.BLACK, ProductOption.S);
    Arc arc = Arc.create(visit.getId(), UUID.randomUUID(), customer.getId(), staffId);
    ArcRevision first =
        ArcRevision.start(arc.getId(), 1, snapshot(variant.getId()), "arc-v1", staffId);
    first.complete(
        """
        {"momentSummary":"첫 요약","preferences":["첫 선호"],"momentToRemember":"첫 기억"}
        """,
        java.time.Instant.parse("2026-08-15T12:00:00Z"));
    ArcRevision finalRevision =
        ArcRevision.start(arc.getId(), 2, snapshot(variant.getId()), "arc-v1", staffId);
    finalRevision.complete(
        """
        {"momentSummary":"최종 요약","preferences":["최종 선호"],"momentToRemember":"최종 기억"}
        """,
        java.time.Instant.parse("2026-08-15T12:02:00Z"));
    arc.shareFirst(first, java.time.Instant.parse("2026-08-15T12:01:00Z"), 1);
    arc.reshare(finalRevision, java.time.Instant.parse("2026-08-15T12:03:00Z"));
    arc.finalizeSharedRevision(java.time.Instant.parse("2026-08-15T12:04:00Z"));

    when(customerRepository.findByTokenHash(tokenManager.hash(rawToken)))
        .thenReturn(Optional.of(customer));
    when(arcRepository.findByIdAndCustomerIdAndStatusIn(
            arc.getId(), customer.getId(), List.of(ArcStatus.SHARED, ArcStatus.FINALIZED)))
        .thenReturn(Optional.of(arc));
    when(arcRevisionRepository.findById(finalRevision.getId()))
        .thenReturn(Optional.of(finalRevision));
    when(visitRepository.findById(visit.getId())).thenReturn(Optional.of(visit));
    when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));
    when(productVariantRepository.findAllById(any())).thenReturn(List.of(variant));
    when(productRepository.findAllById(any())).thenReturn(List.of(product));
    customer.updateName("Ethan");

    CustomerArcQueryService.ArcDetail result = service.getArc(arc.getId(), rawToken);

    assertThat(result.arcNumber()).isEqualTo(1);
    assertThat(result.customerName()).isEqualTo("Ethan");
    assertThat(result.momentSummary()).isEqualTo("최종 요약");
    assertThat(result.purchasedProducts())
        .singleElement()
        .extracting(CustomerArcQueryService.ProductView::productName)
        .isEqualTo("A Bag");
  }

  @Test
  void 다른_고객의_Arc_상세는_찾을_수_없는_것처럼_처리한다() {
    String rawToken = "known-token";
    Customer customer = Customer.create(tokenManager.hash(rawToken));
    UUID arcId = UUID.randomUUID();
    when(customerRepository.findByTokenHash(tokenManager.hash(rawToken)))
        .thenReturn(Optional.of(customer));
    when(arcRepository.findByIdAndCustomerIdAndStatusIn(
            arcId, customer.getId(), List.of(ArcStatus.SHARED, ArcStatus.FINALIZED)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getArc(arcId, rawToken))
        .isInstanceOf(BusinessException.class)
        .extracting(exception -> ((BusinessException) exception).errorCode())
        .isEqualTo(CommonErrorCode.NOT_FOUND);
  }

  private ArcInputSnapshot snapshot(UUID purchasedVariantId) {
    return new ArcInputSnapshot(
        List.of(purchasedVariantId),
        java.util.Set.of(ProductCategory.BAG),
        java.util.Set.of(PreferredColor.BLACK),
        null,
        java.util.Set.of(PreferredStyle.CLASSIC_TIMELESS),
        null,
        List.of(),
        java.util.Set.of(PurchaseCriterion.DESIGN),
        null,
        java.util.Set.of(ActualInteractionPreference.MODERATE_GUIDANCE),
        java.util.Set.of(ProductExplanationPreference.KEY_POINTS_ONLY),
        PurchaseDecisionStyle.COMPARE_FIRST,
        "재방문 시 신상품 안내");
  }
}
