package com.tickatch.auth_service.auth.presentation.api.dto.request;

import com.tickatch.auth_service.auth.domain.vo.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 이메일 중복 확인 요청 DTO.
 *
 * @param email 이메일
 * @param userType 사용자 유형
 */
public record CheckEmailRequest(
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    String email,

    @NotNull(message = "사용자 유형은 필수입니다")
    UserType userType
) {
}