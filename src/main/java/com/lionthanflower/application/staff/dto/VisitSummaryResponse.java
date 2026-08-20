// 방문 고객 목록의 각 항목을 담는 DTO
package com.lionthanflower.application.staff.dto;

import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.visit.entity.InteractionStyle;
import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import java.time.Instant;
import java.util.UUID;

public record VisitSummaryResponse(
    UUID visitId,
    String customerName,
    VisitStatus status,
    LanguageCode serviceLanguage,
    InteractionStyle interactionStyle,
    UUID staffId,
    String additionalRequest,
    long arcCount,
    UUID arcId,
    UUID visitMemoryId,
    Instant matchedAt,
    Instant visitedAt,
    Instant completedAt,
    VisitResultType resultType,
    UUID resultId,
    Integer arcNumber) {

  public static VisitSummaryResponse of(Visit visit, String customerName, long arcCount) {
    return of(visit, customerName, arcCount, null, null);
  }

  public static VisitSummaryResponse of(
      Visit visit, String customerName, long arcCount, UUID arcId, UUID visitMemoryId) {
    return of(visit, customerName, arcCount, arcId, visitMemoryId, null, null, null);
  }

  public static VisitSummaryResponse of(
      Visit visit,
      String customerName,
      long arcCount,
      UUID arcId,
      UUID visitMemoryId,
      VisitResultType resultType,
      UUID resultId,
      Integer arcNumber) {
    return new VisitSummaryResponse(
        visit.getId(),
        customerName,
        visit.getStatus(),
        visit.getServiceLanguage(),
        visit.getInteractionStyle(),
        visit.getStaffId(),
        visit.getAdditionalRequest(),
        arcCount,
        arcId,
        visitMemoryId,
        visit.getMatchedAt(),
        visit.getCreatedAt(),
        visit.getCompletedAt(),
        resultType,
        resultId,
        arcNumber);
  }
}
