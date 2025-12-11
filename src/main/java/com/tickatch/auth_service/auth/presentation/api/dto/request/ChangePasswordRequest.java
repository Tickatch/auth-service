package com.tickatch.auth_service.auth.presentation.api.dto.request;

import com.tickatch.auth_service.auth.application.service.command.dto.ChangePasswordCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * 비밀번호 변경 요청 DTO.
 *
 * @param currentPassword 현재 비밀번호
 * @param newPassword 새 비밀번호
 */
public record ChangePasswordRequest(
    @NotBlank(message = "현재 비밀번호는 필수입니다")
    String currentPassword,

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하여야 합니다")
    String newPassword
) {

  /**
   * Command로 변환한다.
   *
   * @param authId Auth ID (인증된 사용자에서 추출)
   * @return ChangePasswordCommand
   */
  public ChangePasswordCommand toCommand(UUID authId) {
    return ChangePasswordCommand.of(authId, currentPassword, newPassword);
  }
}