// OpenAI RestClient의 연결과 읽기 타임아웃 구성을 검증하는 테스트
package com.lionthanflower.infrastructure.openai;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

class OpenAiClientConfigTest {

  @Test
  void RestClient에_설정한_연결과_읽기_타임아웃을_적용한다() throws Exception {
    Method factoryMethod =
        Arrays.stream(OpenAiClientConfig.class.getDeclaredMethods())
            .filter(method -> method.getName().equals("openAiRestClient"))
            .filter(method -> method.getParameterCount() == 3)
            .findFirst()
            .orElseThrow();

    RestClient restClient =
        (RestClient)
            factoryMethod.invoke(
                new OpenAiClientConfig(),
                "http://localhost",
                Duration.ofSeconds(2),
                Duration.ofSeconds(3));
    Object requestFactory = ReflectionTestUtils.getField(restClient, "clientRequestFactory");

    assertThat(requestFactory).isInstanceOf(SimpleClientHttpRequestFactory.class);
    assertThat(ReflectionTestUtils.getField(requestFactory, "connectTimeout")).isEqualTo(2_000);
    assertThat(ReflectionTestUtils.getField(requestFactory, "readTimeout")).isEqualTo(3_000);
  }
}
