package com.tickatch.auth_service.auth.presentation.api.dto.request;

import com.tickatch.auth_service.auth.application.service.command.dto.LogoutCommand;
import java.util.UUID;

/**
 * 로그아웃 요청 DTO.
 *
 * @param refreshToken Refresh Token (현재 세션)
 * @param allDevices 모든 기기에서 로그아웃 여부
 */
public record LogoutRequest(
    String refreshToken,
    boolean allDevices
) {

  /**
   * Command로 변환한다.
   *
   * @param authId Auth ID (인증된 사용자에서 추출)
   * @return LogoutCommand
   */
  public LogoutCommand toCommand(UUID authId) {
    return LogoutCommand.of(authId, refreshToken, allDevices);
  }
}