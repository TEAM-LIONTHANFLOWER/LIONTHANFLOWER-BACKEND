// 구매 날짜·국가·매장을 포함한 직원 Arc 생성과 재생성 요청 값
package com.lionthanflower.application.staff.dto;

import com.lionthanflower.domain.arc.entity.ArcInputSnapshot;

public record StaffArcGenerationRequest(ArcInputSnapshot inputSnapshot) {}
