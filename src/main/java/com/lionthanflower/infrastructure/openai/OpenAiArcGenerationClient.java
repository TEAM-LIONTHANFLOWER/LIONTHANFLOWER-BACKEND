// OpenAI Responses API를 사용해 Arc 생성 콘텐츠를 만드는 어댑터
package com.lionthanflower.infrastructure.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.lionthanflower.application.arc.ArcGenerationCommand;
import com.lionthanflower.application.arc.ArcGenerationPort;
import com.lionthanflower.domain.arc.entity.ArcGeneratedContent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiArcGenerationClient implements ArcGenerationPort {

  private static final String SYSTEM_PROMPT =
      "당신은 럭셔리 매장 직원의 고객 경험을 Arc 형식으로 기록하는 작가입니다. "
          + "입력된 구매 정보와 고객 선호, 직원 관찰을 바탕으로 한국어 콘텐츠를 JSON으로 작성합니다.";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final String apiKey;
  private final String model;

  public OpenAiArcGenerationClient(
      RestClient restClient,
      @Value("${app.openai.api-key:}") String apiKey,
      @Value("${app.openai.model:gpt-4o-mini}") String model) {
    this.restClient = restClient;
    this.objectMapper =
        new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    this.apiKey = apiKey;
    this.model = model;
  }

  @Override
  public ArcGeneratedContent generate(ArcGenerationCommand command) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("OpenAI API 키가 설정되지 않았습니다.");
    }

    try {
      String response =
          restClient
              .post()
              .uri("/v1/responses")
              .contentType(MediaType.APPLICATION_JSON)
              .header("Authorization", "Bearer " + apiKey)
              .body(requestBody(command))
              .retrieve()
              .body(String.class);
      return parseResponse(response);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Arc 생성 결과를 해석할 수 없습니다.", exception);
    } catch (RuntimeException exception) {
      if (exception instanceof IllegalStateException) {
        throw exception;
      }
      throw new IllegalStateException("OpenAI Arc 생성 요청에 실패했습니다.", exception);
    }
  }

  private Map<String, Object> requestBody(ArcGenerationCommand command)
      throws JsonProcessingException {
    Map<String, Object> context = new LinkedHashMap<>();
    context.put("customerName", command.customerName());
    if (command.additionalRequest() != null) {
      context.put("additionalRequest", command.additionalRequest());
    }
    context.put("arcInput", command.inputSnapshot());

    Map<String, Object> userContent =
        Map.of(
            "role",
            "user",
            "content",
            List.of(
                Map.of("type", "input_text", "text", objectMapper.writeValueAsString(context))));
    Map<String, Object> systemContent =
        Map.of(
            "role",
            "system",
            "content",
            List.of(Map.of("type", "input_text", "text", SYSTEM_PROMPT)));

    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("additionalProperties", false);
    schema.put(
        "properties",
        Map.of(
            "momentSummary", Map.of("type", "string"),
            "preferences", Map.of("type", "array", "items", Map.of("type", "string")),
            "momentToRemember", Map.of("type", "string")));
    schema.put("required", List.of("momentSummary", "preferences", "momentToRemember"));

    Map<String, Object> format = new LinkedHashMap<>();
    format.put("type", "json_schema");
    format.put("name", "arc_generated_content");
    format.put("strict", true);
    format.put("schema", schema);

    return Map.of(
        "model", model,
        "input", List.of(systemContent, userContent),
        "text", Map.of("format", format));
  }

  private ArcGeneratedContent parseResponse(String response) throws JsonProcessingException {
    if (response == null || response.isBlank()) {
      throw new IllegalStateException("Arc 생성 결과가 비어 있습니다.");
    }
    JsonNode root = objectMapper.readTree(response);
    String outputText = root.path("output_text").asText(null);
    if (outputText == null || outputText.isBlank()) {
      outputText = findOutputText(root.path("output"));
    }
    if (outputText == null || outputText.isBlank()) {
      throw new IllegalStateException("Arc 생성 결과가 없습니다.");
    }
    return objectMapper.readValue(outputText, ArcGeneratedContent.class);
  }

  private String findOutputText(JsonNode output) {
    if (!output.isArray()) {
      return null;
    }
    List<String> texts = new ArrayList<>();
    output.forEach(
        item ->
            item.path("content")
                .forEach(
                    content -> {
                      if (!"output_text".equals(content.path("type").asText())) {
                        return;
                      }
                      String text = content.path("text").asText(null);
                      if (text != null && !text.isBlank()) {
                        texts.add(text);
                      }
                    }));
    return texts.isEmpty() ? null : texts.getFirst();
  }
}
