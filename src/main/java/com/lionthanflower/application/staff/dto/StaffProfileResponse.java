// 직원 프로필 응답 값을 담는 DTO
package com.lionthanflower.application.staff.dto;

import com.lionthanflower.domain.common.entity.LanguageCode;
import com.lionthanflower.domain.store.entity.Staff;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record StaffProfileResponse(
    UUID staffId, UUID storeId, String name, Set<LanguageCode> languages, Instant createdAt) {
  public static StaffProfileResponse from(Staff staff) {
    return new StaffProfileResponse(
        staff.getId(),
        staff.getStoreId(),
        staff.getName(),
        staff.getLanguages(),
        staff.getCreatedAt());
  }
}
