// 직원 Visit Memory 최초 생성과 재생성 입력을 전달하는 요청 DTO
package com.lionthanflower.application.staff.dto;

import com.lionthanflower.domain.visitmemory.entity.VisitMemoryInputSnapshot;

public record StaffVisitMemoryGenerationRequest(VisitMemoryInputSnapshot inputSnapshot) {}
