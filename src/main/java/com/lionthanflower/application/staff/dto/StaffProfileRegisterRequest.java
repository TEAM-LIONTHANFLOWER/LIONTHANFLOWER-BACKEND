// 직원 프로필 등록 요청 값을 담는 DTO
package com.lionthanflower.application.staff.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record StaffProfileRegisterRequest(
    @NotNull UUID storeId,
    @NotBlank @Size(max = 100) String name,
    @NotEmpty Set<@NotBlank String> languages) {}
