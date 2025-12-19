package com.tickatch.auth_service.auth.application.service.command.dto;

import com.tickatch.auth_service.auth.domain.vo.UserType;

/**
 * 로그인 요청 커맨드.
 *
 * @param email 이메일
 * @param password 비밀번호
 * @param userType 사용자 유형
 * @param deviceInfo 디바이스 정보
 * @param rememberMe 로그인 유지 여부
 */
public record LoginCommand(
    String email, String password, UserType userType, String deviceInfo, boolean rememberMe) {

  public static LoginCommand of(
      String email, String password, UserType userType, String deviceInfo, boolean rememberMe) {
    return new LoginCommand(email, password, userType, deviceInfo, rememberMe);
  }
}
