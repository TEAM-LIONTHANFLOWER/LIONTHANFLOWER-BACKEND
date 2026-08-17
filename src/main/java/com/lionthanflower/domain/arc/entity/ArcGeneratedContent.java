// 고객 Arc 화면에 표시할 생성 콘텐츠의 구조와 필수 값을 검증하는 불변 객체
package com.lionthanflower.domain.arc.entity;

import java.util.List;

public record ArcGeneratedContent(
    String momentSummary, List<String> preferences, String momentToRemember) {

  public ArcGeneratedContent {
    momentSummary = requireText(momentSummary, "MCM Moment 요약");
    momentToRemember = requireText(momentToRemember, "기억할 순간");
    if (preferences == null || preferences.isEmpty()) {
      throw new IllegalArgumentException("고객 선호는 하나 이상이어야 합니다.");
    }
    preferences = preferences.stream().map(value -> requireText(value, "고객 선호")).toList();
  }

  private static String requireText(String value, String fieldName) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(fieldName + "은 비어 있을 수 없습니다.");
    }
    return value.trim();
  }
}
