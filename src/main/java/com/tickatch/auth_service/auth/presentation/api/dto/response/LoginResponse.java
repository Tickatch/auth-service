package com.tickatch.auth_service.auth.presentation.api.dto.response;

import com.tickatch.auth_service.auth.application.service.command.dto.LoginResult;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 로그인 응답 DTO.
 *
 * @param authId Auth ID (= userId)
 * @param email 이메일
 * @param userType 사용자 유형
 * @param accessToken Access Token
 * @param refreshToken Refresh Token
 * @param accessTokenExpiresAt Access Token 만료 시간
 * @param refreshTokenExpiresAt Refresh Token 만료 시간
 */
public record LoginResponse(
    UUID authId,
    String email,
    UserType userType,
    String accessToken,
    String refreshToken,
    LocalDateTime accessTokenExpiresAt,
    LocalDateTime refreshTokenExpiresAt
) {

  /**
   * LoginResult에서 변환한다.
   *
   * @param result 로그인 결과
   * @return LoginResponse
   */
  public static LoginResponse from(LoginResult result) {
    return new LoginResponse(
        result.authId(),
        result.email(),
        result.userType(),
        result.accessToken(),
        result.refreshToken(),
        result.accessTokenExpiresAt(),
        result.refreshTokenExpiresAt()
    );
  }
}