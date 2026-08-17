// 직원 프로필 등록 요청 값을 담는 DTO
package com.lionthanflower.application.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;

public record StaffProfileRegisterRequest(
    @NotNull UUID storeId, @NotBlank String name, @NotEmpty Set<String> languages) {}
