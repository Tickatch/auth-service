package com.tickatch.auth_service.auth.presentation.api.dto.request;

import com.tickatch.auth_service.auth.application.service.command.dto.WithdrawCommand;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

/**
 * 회원탈퇴 요청 DTO.
 *
 * @param password 비밀번호 (본인 확인용)
 */
public record WithdrawRequest(@NotBlank(message = "비밀번호는 필수입니다") String password) {

  /**
   * Command로 변환한다.
   *
   * @param authId Auth ID (인증된 사용자에서 추출)
   * @return WithdrawCommand
   */
  public WithdrawCommand toCommand(UUID authId) {
    return WithdrawCommand.of(authId, password);
  }
}
