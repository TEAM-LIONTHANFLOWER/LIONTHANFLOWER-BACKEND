// 실제 매장 저장부터 고객 Arc 도시 응답과 API 문서까지 검증하는 통합 테스트
package com.lionthanflower.infrastructure.web.customer;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.lionthanflower.application.customer.CustomerTokenManager;
import com.lionthanflower.domain.arc.entity.Arc;
import com.lionthanflower.domain.arc.entity.ArcInputSnapshot;
import com.lionthanflower.domain.arc.entity.ArcRevision;
import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.customer.entity.Customer;
import com.lionthanflower.domain.product.entity.Product;
import com.lionthanflower.domain.product.entity.ProductCategory;
import com.lionthanflower.domain.product.entity.ProductColor;
import com.lionthanflower.domain.product.entity.ProductOption;
import com.lionthanflower.domain.product.entity.ProductVariant;
import com.lionthanflower.domain.purchase.entity.Purchase;
import com.lionthanflower.domain.purchase.entity.PurchaseItem;
import com.lionthanflower.domain.store.entity.Staff;
import com.lionthanflower.domain.store.entity.Store;
import com.lionthanflower.domain.visit.entity.InteractionStyle;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.support.PostgreSqlContainerSupport;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerArcCityIntegrationTest extends PostgreSqlContainerSupport {

  @Autowired private MockMvc mockMvc;
  @Autowired private EntityManager entityManager;
  @Autowired private CustomerTokenManager tokenManager;

  @Test
  void 여러_매장의_공개_Arc는_방문_매장의_도시를_목록과_상세에_반환한다() throws Exception {
    String rawToken = UUID.randomUUID().toString();
    Customer customer = Customer.create(tokenManager.hash(rawToken));
    customer.updateName("Alex");
    entityManager.persist(customer);
    Product product = Product.create(UUID.randomUUID().toString(), "City Bag", ProductCategory.BAG);
    entityManager.persist(product);
    ProductVariant variant =
        ProductVariant.create(
            product.getId(), UUID.randomUUID().toString(), ProductColor.BLACK, ProductOption.S);
    entityManager.persist(variant);

    UUID seoulArc = persistArc(customer, variant, "MCM Seoul", "KR", " seoul ", 1);
    UUID parisArc = persistArc(customer, variant, "MCM Paris", "FR", "PARIS", 2);
    UUID munichArc = persistArc(customer, variant, "MCM Munich", "DE", "MUNICH", 3);
    UUID unknownArc = persistArc(customer, variant, "MCM Seoul", "KR", null, 4);
    entityManager.flush();
    entityManager.clear();

    Cookie cookie = new Cookie("customer_token", rawToken);
    mockMvc
        .perform(get("/api/customers/arcs").cookie(cookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.length()").value(4))
        .andExpect(jsonPath("$.data[0].arcId").value(unknownArc.toString()))
        .andExpect(jsonPath("$.data[0].cityCode").hasJsonPath())
        .andExpect(jsonPath("$.data[0].cityCode").value(org.hamcrest.Matchers.nullValue()))
        .andExpect(jsonPath("$.data[1].arcId").value(munichArc.toString()))
        .andExpect(jsonPath("$.data[1].cityCode").value("MUNICH"))
        .andExpect(jsonPath("$.data[2].arcId").value(parisArc.toString()))
        .andExpect(jsonPath("$.data[2].cityCode").value("PARIS"))
        .andExpect(jsonPath("$.data[3].arcId").value(seoulArc.toString()))
        .andExpect(jsonPath("$.data[3].cityCode").value("SEOUL"));
    assertDetail(cookie, seoulArc, "MCM Seoul", "SEOUL", "KR");
    assertDetail(cookie, parisArc, "MCM Paris", "PARIS", "FR");
    assertDetail(cookie, munichArc, "MCM Munich", "MUNICH", "DE");
    assertDetail(cookie, unknownArc, "MCM Seoul", null, "KR");
  }

  @Test
  void 생성된_OpenAPI에_목록과_상세의_nullable_도시_코드가_노출된다() throws Exception {
    for (String schema : List.of("ArcListItemResponse", "ArcDetailResponse")) {
      String path = "$.components.schemas." + schema + ".properties.cityCode";
      mockMvc
          .perform(get("/v3/api-docs"))
          .andExpect(status().isOk())
          .andExpect(jsonPath(path + ".type").value(containsInAnyOrder("string", "null")))
          .andExpect(jsonPath(path + ".example").value("SEOUL"))
          .andExpect(jsonPath(path + ".maxLength").value(100))
          .andExpect(
              jsonPath(path + ".description").value(org.hamcrest.Matchers.containsString("방문")));
    }
  }

  private UUID persistArc(
      Customer customer,
      ProductVariant variant,
      String storeName,
      String countryCode,
      String cityCode,
      int arcNumber) {
    Store store = Store.create(storeName, UUID.randomUUID().toString(), countryCode, cityCode);
    entityManager.persist(store);
    Staff staff =
        Staff.create(store.getId(), "Staff", UUID.randomUUID().toString(), Set.of(LanguageCode.EN));
    entityManager.persist(staff);
    Visit visit = Visit.create(customer.getId(), store.getId());
    Instant now = Instant.now();
    visit.completeOnboarding(LanguageCode.EN, InteractionStyle.SELF_GUIDED, null);
    visit.assignStaff(staff.getId(), now);
    visit.confirmPurchase(staff.getId(), now);
    visit.complete(now);
    entityManager.persist(visit);
    Purchase purchase = Purchase.create(visit.getId());
    entityManager.persist(purchase);
    entityManager.persist(PurchaseItem.create(purchase.getId(), variant.getId()));
    Arc arc = Arc.create(visit.getId(), purchase.getId(), customer.getId(), staff.getId());
    entityManager.persist(arc);
    entityManager.flush();
    ArcInputSnapshot snapshot =
        new ArcInputSnapshot(
            LocalDate.now(),
            "OTHER COUNTRY",
            "OTHER STORE",
            List.of(variant.getId()),
            Set.of(),
            Set.of(),
            null,
            Set.of(),
            null,
            List.of(),
            Set.of(),
            null,
            Set.of(),
            Set.of(),
            null,
            null);
    ArcRevision revision = ArcRevision.start(arc.getId(), 1, snapshot, "arc-v1", staff.getId());
    revision.complete(
        """
        {"momentSummary":"Your visit","preferences":["Practical design"],"momentToRemember":"A special day"}
        """,
        now);
    entityManager.persist(revision);
    entityManager.flush();
    arc.shareFirst(revision, now, arcNumber);
    if (arcNumber == 2) {
      arc.finalizeSharedRevision(now);
    }
    return arc.getId();
  }

  private void assertDetail(
      Cookie cookie, UUID arcId, String storeName, String cityCode, String countryCode)
      throws Exception {
    mockMvc
        .perform(get("/api/customers/arcs/{arcId}", arcId).cookie(cookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.storeName").value(storeName))
        .andExpect(jsonPath("$.data.cityCode").hasJsonPath())
        .andExpect(jsonPath("$.data.cityCode").value(cityCode))
        .andExpect(jsonPath("$.data.countryCode").value(countryCode))
        .andExpect(jsonPath("$.data.momentToRemember").value("A special day"))
        .andExpect(jsonPath("$.data.purchasedProducts[0].productName").value("City Bag"));
  }
}
