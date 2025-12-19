package com.tickatch.auth_service.auth.application.service.command.dto;

import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;

/**
 * 소셜 로그인 요청 커맨드.
 *
 * @param providerType 소셜 로그인 제공자
 * @param providerUserId 제공자 측 사용자 ID
 * @param userType 사용자 유형
 * @param deviceInfo 디바이스 정보
 * @param rememberMe 로그인 유지 여부
 */
public record OAuthLoginCommand(
    ProviderType providerType,
    String providerUserId,
    UserType userType,
    String deviceInfo,
    boolean rememberMe) {

  public static OAuthLoginCommand of(
      ProviderType providerType,
      String providerUserId,
      UserType userType,
      String deviceInfo,
      boolean rememberMe) {
    return new OAuthLoginCommand(providerType, providerUserId, userType, deviceInfo, rememberMe);
  }
}
