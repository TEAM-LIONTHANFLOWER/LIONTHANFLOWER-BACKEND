// 방문 고객 목록 응답을 감싸는 DTO
package com.lionthanflower.application.staff.dto;

import java.util.List;

public record StaffVisitListResponse(List<VisitSummaryResponse> visits) {}
