package com.tickatch.auth_service.token.application.service.command.dto;

import java.time.LocalDateTime;

/**
 * 토큰 발급 결과.
 *
 * @param accessToken Access Token
 * @param refreshToken Refresh Token
 * @param accessTokenExpiresAt Access Token 만료 시간
 * @param refreshTokenExpiresAt Refresh Token 만료 시간
 */
public record TokenResult(
    String accessToken,
    String refreshToken,
    LocalDateTime accessTokenExpiresAt,
    LocalDateTime refreshTokenExpiresAt) {

  public static TokenResult of(
      String accessToken,
      String refreshToken,
      LocalDateTime accessTokenExpiresAt,
      LocalDateTime refreshTokenExpiresAt) {
    return new TokenResult(accessToken, refreshToken, accessTokenExpiresAt, refreshTokenExpiresAt);
  }
}
