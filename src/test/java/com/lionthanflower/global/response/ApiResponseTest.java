// API 성공 응답의 생성 규칙을 검증하는 테스트
package com.lionthanflower.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void success는_전달된_데이터로_성공_응답을_생성한다() {
    TestData data = new TestData("example");

    ApiResponse<TestData> response = ApiResponse.success(data);

    assertThat(response.success()).isTrue();
    assertThat(response.data()).isSameAs(data);
  }

  @Test
  void 성공_응답은_표준_JSON_구조로_직렬화된다() throws Exception {
    JsonNode json =
        objectMapper.readTree(
            objectMapper.writeValueAsString(ApiResponse.success(new TestData("example"))));

    assertThat(json.get("success").asBoolean()).isTrue();
    assertThat(json.get("data").get("value").asText()).isEqualTo("example");
  }

  private record TestData(String value) {}
}
