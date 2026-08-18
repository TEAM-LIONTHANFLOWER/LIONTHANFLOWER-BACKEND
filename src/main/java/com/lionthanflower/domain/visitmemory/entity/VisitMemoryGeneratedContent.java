// Visit Memory OpenAI 생성 결과의 구조화된 요약 콘텐츠를 표현하는 값 객체
package com.lionthanflower.domain.visitmemory.entity;

public record VisitMemoryGeneratedContent(String summary) {

  public VisitMemoryGeneratedContent {
    if (summary == null || summary.isBlank()) {
      throw new IllegalArgumentException("Visit Memory 요약은 비어 있을 수 없습니다.");
    }
    summary = summary.trim();
  }
}
