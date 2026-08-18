// OpenAI Visit Memory 생성을 위해 직원 입력과 고객 맥락을 전달하는 명령 객체
package com.lionthanflower.application.visitmemory;

import com.lionthanflower.domain.visitmemory.entity.VisitMemoryInputSnapshot;
import java.util.Objects;

public record VisitMemoryGenerationCommand(
    String customerName, String additionalRequest, VisitMemoryInputSnapshot inputSnapshot) {

  public VisitMemoryGenerationCommand {
    customerName = requireText(customerName, "고객 이름");
    inputSnapshot = Objects.requireNonNull(inputSnapshot, "Visit Memory 입력은 null일 수 없습니다.");
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
