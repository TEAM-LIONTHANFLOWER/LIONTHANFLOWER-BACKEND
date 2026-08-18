// 직원 Visit Memory 미리보기와 공유 결과를 반환하는 응답 DTO
package com.lionthanflower.application.staff.dto;

import com.lionthanflower.domain.visitmemory.entity.VisitMemoryGeneratedContent;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryInputSnapshot;
import com.lionthanflower.domain.visitmemory.entity.VisitMemoryStatus;
import java.time.Instant;
import java.util.UUID;

public record StaffVisitMemoryResponse(
    UUID visitMemoryId,
    UUID visitId,
    VisitMemoryStatus status,
    VisitMemoryInputSnapshot inputSnapshot,
    VisitMemoryGeneratedContent generatedContent,
    String failureCode,
    Instant generatedAt,
    Instant finalizedAt) {}
