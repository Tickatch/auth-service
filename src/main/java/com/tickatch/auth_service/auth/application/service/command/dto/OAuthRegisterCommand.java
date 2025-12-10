package com.tickatch.auth_service.auth.application.service.command.dto;

import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;

/**
 * 소셜 회원가입 요청 커맨드.
 *
 * @param email 이메일
 * @param password 비밀번호
 * @param userType 사용자 유형
 * @param providerType 소셜 로그인 제공자
 * @param providerUserId 제공자 측 사용자 ID
 * @param deviceInfo 디바이스 정보
 * @param rememberMe 로그인 유지 여부
 */
public record OAuthRegisterCommand(
    String email,
    String password,
    UserType userType,
    ProviderType providerType,
    String providerUserId,
    String deviceInfo,
    boolean rememberMe
) {

  public static OAuthRegisterCommand of(
      String email,
      String password,
      UserType userType,
      ProviderType providerType,
      String providerUserId,
      String deviceInfo,
      boolean rememberMe
  ) {
    return new OAuthRegisterCommand(
        email, password, userType, providerType, providerUserId, deviceInfo, rememberMe
    );
  }
}