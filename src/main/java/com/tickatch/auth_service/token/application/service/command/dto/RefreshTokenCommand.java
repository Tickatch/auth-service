package com.tickatch.auth_service.token.application.service.command.dto;

/**
 * 토큰 갱신 요청 커맨드.
 *
 * @param refreshToken Refresh Token 값
 */
public record RefreshTokenCommand(String refreshToken) {

  public static RefreshTokenCommand of(String refreshToken) {
    return new RefreshTokenCommand(refreshToken);
  }
}
