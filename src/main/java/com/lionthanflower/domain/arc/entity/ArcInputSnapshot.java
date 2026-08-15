// Arc 생성에 사용하는 구매와 고객 선호 및 직원 관찰 입력을 검증하는 불변 스냅샷
package com.lionthanflower.domain.arc.entity;

import com.lionthanflower.domain.product.entity.ProductCategory;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record ArcInputSnapshot(
    List<UUID> purchasedProductVariantIds,
    Set<ProductCategory> preferredCategories,
    Set<PreferredColor> preferredColors,
    String preferredColorOther,
    Set<PreferredStyle> preferredStyles,
    String preferredStyleOther,
    List<UUID> interestedProductVariantIds,
    Set<PurchaseCriterion> purchaseCriteria,
    String purchaseCriterionOther,
    Set<ActualInteractionPreference> interactionPreferences,
    Set<ProductExplanationPreference> explanationPreferences,
    PurchaseDecisionStyle purchaseDecisionStyle,
    String staffObservation) {

  public ArcInputSnapshot {
    purchasedProductVariantIds = copyList(purchasedProductVariantIds, true, "구매 제품 Variant");
    preferredCategories = copySet(preferredCategories, "선호 제품군");
    preferredColors = copySet(preferredColors, "선호 컬러");
    preferredColorOther = normalizeOptional(preferredColorOther, 100, "기타 선호 컬러");
    preferredStyles = copySet(preferredStyles, "선호 스타일");
    preferredStyleOther = normalizeOptional(preferredStyleOther, 100, "기타 선호 스타일");
    interestedProductVariantIds = copyList(interestedProductVariantIds, false, "관심 제품 Variant");
    purchaseCriteria = copySet(purchaseCriteria, "구매 기준");
    purchaseCriterionOther = normalizeOptional(purchaseCriterionOther, 100, "기타 구매 기준");
    interactionPreferences = copySet(interactionPreferences, "선호 응대 방식");
    explanationPreferences = copySet(explanationPreferences, "제품 설명 선호");
    staffObservation = normalizeOptional(staffObservation, 200, "직원 관찰 메모");
  }

  private static <T> List<T> copyList(List<T> values, boolean required, String fieldName) {
    if (values == null || (required && values.isEmpty())) {
      throw new IllegalArgumentException(fieldName + "은 하나 이상이어야 합니다.");
    }
    if (values.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException(fieldName + "에는 null이 포함될 수 없습니다.");
    }
    return List.copyOf(values);
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
