// OpenAI Visit Memory 생성 응답 변환과 실패 처리를 검증하는 테스트
package com.lionthanflower.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lionthanflower.application.visitmemory.VisitMemoryGenerationCommand;
import com.lionthanflower.domain.visitmemory.entity.CustomerInterestPoint;
import com.lionthanflower.domain.visitmemory.entity.NoPurchaseReason;
import com.lionthanflower.domain.visitmemory.entity.ProductEngagement;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryInputSnapshot;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiVisitMemoryGenerationClientTest {

  private MockRestServiceServer server;
  private OpenAiVisitMemoryGenerationClient client;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    client = new OpenAiVisitMemoryGenerationClient(builder.build(), "test-api-key", "test-model");
  }

  @Test
  void Responses_API의_생성_결과를_Visit_Memory_콘텐츠로_변환한다() {
    server
        .expect(requestTo("/v1/responses"))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("홍길동")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("VIEWED_WITH_INTEREST")))
        .andRespond(
            withSuccess(
                new ObjectMapper()
                    .createObjectNode()
                    .put("output_text", "{\"summary\":\"다음 방문을 준비한 기록\"}")
                    .toString(),
                MediaType.APPLICATION_JSON));

    var result = client.generate(command());

    assertThat(result.summary()).isEqualTo("다음 방문을 준비한 기록");
    server.verify();
  }

  @Test
  void 생성_결과가_Visit_Memory_형식이_아니면_실패한다() {
    server
        .expect(requestTo("/v1/responses"))
        .andRespond(
            withSuccess(
                new ObjectMapper()
                    .createObjectNode()
                    .put("output_text", "{\"wrong\":\"필드\"}")
                    .toString(),
                MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> client.generate(command()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Visit Memory 생성 결과");
    server.verify();
  }

  private VisitMemoryGenerationCommand command() {
    return new VisitMemoryGenerationCommand("홍길동", "다음 방문에 안내", snapshot());
  }

  private VisitMemoryInputSnapshot snapshot() {
    return new VisitMemoryInputSnapshot(
        Map.of(UUID.randomUUID(), Set.of(ProductEngagement.VIEWED_WITH_INTEREST)),
        Set.of(CustomerInterestPoint.DESIGN),
        null,
        Set.of(NoPurchaseReason.NEED_MORE_TIME),
        null,
        "다음 방문에 신상품 안내");
  }
}
