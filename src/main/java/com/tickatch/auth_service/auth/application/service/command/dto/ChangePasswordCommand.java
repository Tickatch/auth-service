package com.tickatch.auth_service.auth.application.service.command.dto;

import java.util.UUID;

/**
 * 비밀번호 변경 요청 커맨드.
 *
 * @param authId Auth ID
 * @param currentPassword 현재 비밀번호
 * @param newPassword 새 비밀번호
 */
public record ChangePasswordCommand(UUID authId, String currentPassword, String newPassword) {

  public static ChangePasswordCommand of(UUID authId, String currentPassword, String newPassword) {
    return new ChangePasswordCommand(authId, currentPassword, newPassword);
  }
}
