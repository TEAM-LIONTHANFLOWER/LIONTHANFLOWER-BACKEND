// OpenAI Arc 생성을 위해 직원 입력과 고객 맥락을 전달하는 명령 객체
package com.lionthanflower.application.arc;

import com.lionthanflower.domain.arc.entity.ArcInputSnapshot;
import com.lionthanflower.domain.common.entity.LanguageCode;
import java.util.Objects;

public record ArcGenerationCommand(
    String customerName,
    LanguageCode serviceLanguage,
    String additionalRequest,
    ArcInputSnapshot inputSnapshot) {

  public ArcGenerationCommand {
    customerName = requireText(customerName, "고객 이름");
    serviceLanguage = Objects.requireNonNull(serviceLanguage, "서비스 언어는 null일 수 없습니다.");
    inputSnapshot = Objects.requireNonNull(inputSnapshot, "Arc 입력은 null일 수 없습니다.");
    additionalRequest = normalizeOptional(additionalRequest);
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + "은 비어 있을 수 없습니다.");
    }
    return value.trim();
  }

  private static String normalizeOptional(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
