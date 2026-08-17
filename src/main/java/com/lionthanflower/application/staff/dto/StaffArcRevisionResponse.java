// 직원이 확인하는 Arc 리비전과 생성 결과를 담는 응답 DTO
package com.lionthanflower.application.staff.dto;

import com.lionthanflower.domain.arc.entity.ArcGeneratedContent;
import com.lionthanflower.domain.arc.entity.ArcInputSnapshot;
import com.lionthanflower.domain.arc.entity.ArcRevisionStatus;
import com.lionthanflower.domain.arc.entity.ArcStatus;
import java.time.Instant;
import java.util.UUID;

public record StaffArcRevisionResponse(
    UUID arcId,
    UUID revisionId,
    int revisionNumber,
    ArcStatus arcStatus,
    ArcRevisionStatus revisionStatus,
    ArcInputSnapshot inputSnapshot,
    ArcGeneratedContent generatedContent,
    String failureCode,
    Instant sharedAt) {}
