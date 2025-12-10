package com.tickatch.auth_service.auth.infrastructure.oauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OAuth 토큰 응답 DTO.
 *
 * @param accessToken 액세스 토큰
 * @param tokenType 토큰 타입 (Bearer)
 * @param expiresIn 만료 시간 (초)
 * @param refreshToken 리프레시 토큰 (일부 제공자만)
 * @param scope 스코프
 */
public record OAuthTokenResponse(
    @JsonProperty("access_token")
    String accessToken,

    @JsonProperty("token_type")
    String tokenType,

    @JsonProperty("expires_in")
    Integer expiresIn,

    @JsonProperty("refresh_token")
    String refreshToken,

    @JsonProperty("scope")
    String scope
) {
}