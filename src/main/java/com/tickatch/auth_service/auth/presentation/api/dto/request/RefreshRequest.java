package com.tickatch.auth_service.auth.presentation.api.dto.request;

import com.tickatch.auth_service.auth.application.service.command.dto.RefreshCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * 토큰 갱신 요청 DTO.
 *
 * @param refreshToken Refresh Token 값
 */
public record RefreshRequest(@NotBlank(message = "Refresh Token은 필수입니다") String refreshToken) {

  /**
   * Command로 변환한다.
   *
   * @return RefreshCommand
   */
  public RefreshCommand toCommand() {
    return RefreshCommand.of(refreshToken);
  }
}
