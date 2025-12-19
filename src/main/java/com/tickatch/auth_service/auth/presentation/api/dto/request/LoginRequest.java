package com.tickatch.auth_service.auth.presentation.api.dto.request;

import com.tickatch.auth_service.auth.application.service.command.dto.LoginCommand;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 로그인 요청 DTO.
 *
 * @param email 이메일
 * @param password 비밀번호
 * @param userType 사용자 유형
 * @param rememberMe 로그인 유지 여부
 */
public record LoginRequest(
    @NotBlank(message = "이메일은 필수입니다") @Email(message = "올바른 이메일 형식이 아닙니다") String email,
    @NotBlank(message = "비밀번호는 필수입니다") String password,
    @NotNull(message = "사용자 유형은 필수입니다") UserType userType,
    boolean rememberMe) {

  /**
   * Command로 변환한다.
   *
   * @param deviceInfo 디바이스 정보 (헤더에서 추출)
   * @return LoginCommand
   */
  public LoginCommand toCommand(String deviceInfo) {
    return LoginCommand.of(email, password, userType, deviceInfo, rememberMe);
  }
}
