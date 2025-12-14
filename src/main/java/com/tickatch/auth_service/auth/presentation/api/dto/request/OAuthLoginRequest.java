package com.tickatch.auth_service.auth.presentation.api.dto.request;

import com.tickatch.auth_service.auth.domain.vo.UserType;
import jakarta.validation.constraints.NotNull;

/**
 * OAuth 로그인 요청 DTO.
 *
 * <p>쿼리 파라미터로 전달받는다.
 *
 * @param userType 사용자 유형 (CUSTOMER만 허용)
 * @param rememberMe 로그인 유지 여부
 */
public record OAuthLoginRequest(
    @NotNull(message = "사용자 유형은 필수입니다")
    UserType userType,

    boolean rememberMe
) {
}