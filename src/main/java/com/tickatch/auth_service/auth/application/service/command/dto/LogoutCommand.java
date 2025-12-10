package com.tickatch.auth_service.auth.application.service.command.dto;

import java.util.UUID;

/**
 * 로그아웃 요청 커맨드.
 *
 * @param authId Auth ID
 * @param refreshToken Refresh Token (현재 세션)
 * @param allDevices 모든 기기에서 로그아웃 여부
 */
public record LogoutCommand(
    UUID authId,
    String refreshToken,
    boolean allDevices
) {

  public static LogoutCommand of(UUID authId, String refreshToken, boolean allDevices) {
    return new LogoutCommand(authId, refreshToken, allDevices);
  }

  /**
   * 현재 기기에서만 로그아웃.
   */
  public static LogoutCommand currentDevice(UUID authId, String refreshToken) {
    return new LogoutCommand(authId, refreshToken, false);
  }

  /**
   * 모든 기기에서 로그아웃.
   */
  public static LogoutCommand allDevices(UUID authId) {
    return new LogoutCommand(authId, null, true);
  }
}