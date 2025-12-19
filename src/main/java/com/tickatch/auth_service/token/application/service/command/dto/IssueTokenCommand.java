package com.tickatch.auth_service.token.application.service.command.dto;

import com.tickatch.auth_service.auth.domain.vo.UserType;
import java.util.UUID;

/**
 * 토큰 발급 요청 커맨드.
 *
 * @param authId 인증 ID
 * @param userType 사용자 유형
 * @param deviceInfo 디바이스 정보
 * @param rememberMe 로그인 유지 여부
 */
public record IssueTokenCommand(
    UUID authId, UserType userType, String deviceInfo, boolean rememberMe) {

  public static IssueTokenCommand of(
      UUID authId, UserType userType, String deviceInfo, boolean rememberMe) {
    return new IssueTokenCommand(authId, userType, deviceInfo, rememberMe);
  }
}
