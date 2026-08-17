// 등록 응답과 발급된 원본 토큰을 함께 담는 DTO
package com.lionthanflower.application.staff.dto;

public record StaffRegistrationResult(StaffProfileResponse profile, String rawToken) {}
