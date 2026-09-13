// 실제 고객 방문 생성 요청이 선택한 매장과 도시 정보에 연결되는지 검증하는 통합 테스트
package com.lionthanflower.infrastructure.web.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lionthanflower.domain.store.entity.Store;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.infrastructure.persistence.StoreRepository;
import com.lionthanflower.infrastructure.persistence.VisitRepository;
import com.lionthanflower.support.PostgreSqlContainerSupport;
import java.util.UUID;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerVisitStoreIntegrationTest extends PostgreSqlContainerSupport {

  @Autowired private MockMvc mockMvc;
  @Autowired private VisitRepository visitRepository;
  @Autowired private StoreRepository storeRepository;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void 고객_방문은_선택한_뮌헨_매장에_연결된다() throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/api/customers/visits").param("storeCode", "MCM-MUNICH"))
            .andExpect(status().isCreated())
            .andReturn();

    UUID visitId =
        UUID.fromString(
            objectMapper
                .readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("visitId")
                .asText());
    Visit visit = visitRepository.findById(visitId).orElseThrow();
    Store store = storeRepository.findById(visit.getStoreId()).orElseThrow();

    assertThat(store.getCode()).isEqualTo("MCM-MUNICH");
    assertThat(store.getCityCode()).isEqualTo("MUNICH");
  }

  @ParameterizedTest
  @ValueSource(strings = {"MCM-SEOUL", "MCM-PARIS", "MCM-MUNICH"})
  void 매장_검색은_등록된_매장을_코드로_반환한다(String storeCode) throws Exception {
    mockMvc
        .perform(get("/api/stores").param("query", storeCode))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[*].code").value(hasItem(storeCode)));
  }

  @Test
  void 생성된_OpenAPI에_선택적_매장_코드가_노출된다() throws Exception {
    MvcResult result = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
    JsonNode parameters =
        objectMapper
            .readTree(result.getResponse().getContentAsString())
            .path("paths")
            .path("/api/customers/visits")
            .path("post")
            .path("parameters");
    JsonNode storeCodeParameter =
        StreamSupport.stream(parameters.spliterator(), false)
            .filter(parameter -> parameter.path("name").asText().equals("storeCode"))
            .findFirst()
            .orElseThrow();

    assertThat(storeCodeParameter.path("in").asText()).isEqualTo("query");
    assertThat(storeCodeParameter.path("required").asBoolean()).isFalse();
    assertThat(storeCodeParameter.path("description").asText()).contains("기본 매장");
    assertThat(storeCodeParameter.path("example").asText()).isEqualTo("MCM-PARIS");
  }
}
