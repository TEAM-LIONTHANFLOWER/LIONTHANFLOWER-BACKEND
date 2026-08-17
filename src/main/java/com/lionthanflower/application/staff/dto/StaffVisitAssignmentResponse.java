// 직원 추천 고객 매칭 결과를 반환하는 응답 DTO
package com.lionthanflower.application.staff.dto;

import com.lionthanflower.domain.visit.entity.Visit;
import com.lionthanflower.domain.visit.entity.VisitStatus;
import java.time.Instant;
import java.util.UUID;

public record StaffVisitAssignmentResponse(
    UUID visitId, UUID staffId, VisitStatus status, Instant matchedAt) {

  public static StaffVisitAssignmentResponse from(Visit visit) {
    return new StaffVisitAssignmentResponse(
        visit.getId(), visit.getStaffId(), visit.getStatus(), visit.getMatchedAt());
  }
}
