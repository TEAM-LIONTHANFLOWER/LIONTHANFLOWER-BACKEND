// OpenAI 호출에 사용하는 RestClient를 구성하는 설정
package com.lionthanflower.infrastructure.openai;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class OpenAiClientConfig {

  @Bean
  RestClient openAiRestClient(
      @Value("${app.openai.base-url:https://api.openai.com}") String baseUrl,
      @Value("${app.openai.connect-timeout:PT5S}") Duration connectTimeout,
      @Value("${app.openai.read-timeout:PT30S}") Duration readTimeout) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeout);
    requestFactory.setReadTimeout(readTimeout);
    return RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
  }
}
