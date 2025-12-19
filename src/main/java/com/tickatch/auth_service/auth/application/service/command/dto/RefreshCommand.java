package com.tickatch.auth_service.auth.application.service.command.dto;

/**
 * 토큰 갱신 요청 커맨드.
 *
 * @param refreshToken Refresh Token 값
 */
public record RefreshCommand(String refreshToken) {

  public static RefreshCommand of(String refreshToken) {
    return new RefreshCommand(refreshToken);
  }
}
