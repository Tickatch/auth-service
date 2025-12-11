package com.tickatch.auth_service.auth.application.service.command.dto;

import com.tickatch.auth_service.auth.domain.vo.UserType;
import com.tickatch.auth_service.token.application.service.command.dto.TokenResult;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 로그인 결과.
 *
 * @param authId Auth ID
 * @param email 이메일
 * @param userType 사용자 유형
 * @param accessToken Access Token
 * @param refreshToken Refresh Token
 * @param accessTokenExpiresAt Access Token 만료 시간
 * @param refreshTokenExpiresAt Refresh Token 만료 시간
 */
public record LoginResult(
    UUID authId,
    String email,
    UserType userType,
    String accessToken,
    String refreshToken,
    LocalDateTime accessTokenExpiresAt,
    LocalDateTime refreshTokenExpiresAt
) {

  public static LoginResult of(
      UUID authId,
      String email,
      UserType userType,
      TokenResult tokenResult
  ) {
    return new LoginResult(
        authId,
        email,
        userType,
        tokenResult.accessToken(),
        tokenResult.refreshToken(),
        tokenResult.accessTokenExpiresAt(),
        tokenResult.refreshTokenExpiresAt()
    );
  }
}