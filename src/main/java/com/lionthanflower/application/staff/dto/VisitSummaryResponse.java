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
    Instant matchedAt,
    Instant visitedAt) {

  public static VisitSummaryResponse of(Visit visit, String customerName, long arcCount) {
    return new VisitSummaryResponse(
        visit.getId(),
        customerName,
        visit.getStatus(),
        visit.getServiceLanguage(),
        visit.getInteractionStyle(),
        visit.getStaffId(),
        visit.getAdditionalRequest(),
        arcCount,
        visit.getMatchedAt(),
        visit.getCreatedAt());
  }
}
