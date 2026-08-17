// OpenAI 호출에 사용하는 RestClient를 구성하는 설정
package com.lionthanflower.infrastructure.openai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiClientConfig {

  @Bean
  RestClient openAiRestClient(
      @Value("${app.openai.base-url:https://api.openai.com}") String baseUrl) {
    return RestClient.builder().baseUrl(baseUrl).build();
  }
}
