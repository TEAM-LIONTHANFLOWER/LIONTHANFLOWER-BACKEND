// OpenAI Arc 생성을 위해 직원 입력과 고객 맥락을 전달하는 명령 객체
package com.lionthanflower.application.arc;

import com.lionthanflower.domain.arc.entity.ArcInputSnapshot;
import java.util.Objects;

public record ArcGenerationCommand(
    String customerName, String additionalRequest, ArcInputSnapshot inputSnapshot) {

  public ArcGenerationCommand {
    customerName = requireText(customerName, "고객 이름");
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
