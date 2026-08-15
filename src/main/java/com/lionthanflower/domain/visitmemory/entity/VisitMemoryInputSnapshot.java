// Visit Memory 생성에 사용하는 제품 행동과 미구매 정보를 검증하는 불변 스냅샷
package com.lionthanflower.domain.visitmemory.entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record VisitMemoryInputSnapshot(
    Map<UUID, Set<ProductEngagement>> productEngagements,
    Set<CustomerInterestPoint> interestPoints,
    String interestPointOther,
    Set<NoPurchaseReason> noPurchaseReasons,
    String noPurchaseReasonOther,
    String nextVisitMemo) {

  public VisitMemoryInputSnapshot {
    productEngagements = copyEngagements(productEngagements);
    interestPoints = copySet(interestPoints, "고객 관심 포인트");
    interestPointOther = normalizeOptional(interestPointOther, 100, "기타 고객 관심 포인트");
    noPurchaseReasons = copySet(noPurchaseReasons, "미구매 사유");
    noPurchaseReasonOther = normalizeOptional(noPurchaseReasonOther, 100, "기타 미구매 사유");
    nextVisitMemo = normalizeOptional(nextVisitMemo, 200, "다음 방문 메모");
  }

  private static Map<UUID, Set<ProductEngagement>> copyEngagements(
      Map<UUID, Set<ProductEngagement>> values) {
    if (values == null) {
      throw new IllegalArgumentException("제품별 고객 행동은 null일 수 없습니다.");
    }
    Map<UUID, Set<ProductEngagement>> copied = new LinkedHashMap<>();
    values.forEach(
        (variantId, engagements) -> {
          if (variantId == null) {
            throw new IllegalArgumentException("관심 제품 Variant ID는 null일 수 없습니다.");
          }
          copied.put(variantId, copySet(engagements, "제품별 고객 행동"));
        });
    return Map.copyOf(copied);
  }

  private static <T> Set<T> copySet(Set<T> values, String fieldName) {
    if (values == null) {
      throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다.");
    }
    if (values.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException(fieldName + "에는 null이 포함될 수 없습니다.");
    }
    return Set.copyOf(values);
  }

  private static String normalizeOptional(String value, int maxLength, String fieldName) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim();
    if (normalized.length() > maxLength) {
      throw new IllegalArgumentException(fieldName + "는 " + maxLength + "자를 초과할 수 없습니다.");
    }
    return normalized;
  }
}
