// OpenAI Arc 생성 응답 변환과 실패 처리를 검증하는 테스트
package com.lionthanflower.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lionthanflower.application.arc.ArcGenerationCommand;
import com.lionthanflower.domain.arc.entity.ActualInteractionPreference;
import com.lionthanflower.domain.arc.entity.ArcGeneratedContent;
import com.lionthanflower.domain.arc.entity.ArcInputSnapshot;
import com.lionthanflower.domain.arc.entity.PreferredColor;
import com.lionthanflower.domain.arc.entity.PreferredStyle;
import com.lionthanflower.domain.arc.entity.ProductExplanationPreference;
import com.lionthanflower.domain.arc.entity.PurchaseCriterion;
import com.lionthanflower.domain.arc.entity.PurchaseDecisionStyle;
import com.lionthanflower.domain.product.entity.ProductCategory;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiArcGenerationClientTest {

  private MockRestServiceServer server;
  private OpenAiArcGenerationClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    client = new OpenAiArcGenerationClient(builder.build(), "test-api-key", "test-model");
  }

  @Test
  void Responses_API의_생성_결과를_Arc_콘텐츠로_변환한다() {
    server
        .expect(requestTo("/v1/responses"))
        .andExpect(method(org.springframework.http.HttpMethod.POST))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("2026-08-13")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("KOREA")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("MCM HAUS")))
        .andRespond(
            withSuccess(
                new ObjectMapper()
                    .createObjectNode()
                    .put(
                        "output_text",
                        "{\"momentSummary\":\"차분한 여행 가방을 발견했습니다.\",\"preferences\":[\"실용적인 디자인\"],\"momentToRemember\":\"다음 여행을 준비한 순간\"}")
                    .toString(),
                MediaType.APPLICATION_JSON));

    ArcGeneratedContent result = client.generate(command());

    assertThat(result.momentSummary()).isEqualTo("차분한 여행 가방을 발견했습니다.");
    assertThat(result.preferences()).containsExactly("실용적인 디자인");
    assertThat(result.momentToRemember()).isEqualTo("다음 여행을 준비한 순간");
    server.verify();
  }

  @Test
  void 생성_결과가_Arc_형식이_아니면_실패한다() {
    server
        .expect(requestTo("/v1/responses"))
        .andRespond(
            withSuccess(
                new ObjectMapper()
                    .createObjectNode()
                    .put("output_text", "{\"momentSummary\":\"요약만 있음\"}")
                    .toString(),
                MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> client.generate(command()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Arc 생성 결과");
    server.verify();
  }

  private ArcGenerationCommand command() {
    return new ArcGenerationCommand("홍길동", "다양한 컬러를 보고 싶어요", snapshot());
  }

  private ArcInputSnapshot snapshot() {
    return new ArcInputSnapshot(
        java.time.LocalDate.of(2026, 8, 13),
        "KOREA",
        "MCM HAUS",
        List.of(java.util.UUID.randomUUID()),
        Set.of(ProductCategory.BAG),
        Set.of(PreferredColor.BLACK),
        null,
        Set.of(PreferredStyle.MINIMAL_SIMPLE),
        null,
        List.of(),
        Set.of(PurchaseCriterion.DESIGN),
        null,
        Set.of(ActualInteractionPreference.ACTIVE_RECOMMENDATION),
        Set.of(ProductExplanationPreference.KEY_POINTS_ONLY),
        PurchaseDecisionStyle.QUICK,
        "차분한 응대를 선호함");
  }
}
