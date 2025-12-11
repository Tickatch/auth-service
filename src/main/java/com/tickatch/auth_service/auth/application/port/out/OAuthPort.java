package com.tickatch.auth_service.auth.application.port.out;

import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.infrastructure.oauth.dto.OAuthUserInfo;

/**
 * OAuth 클라이언트 아웃바운드 포트.
 *
 * <p>OAuth 제공자와의 통신을 추상화한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
public interface OAuthPort {

  /**
   * OAuth 인증 URL을 생성한다.
   *
   * @param providerType 제공자 타입
   * @param state 상태값 (인코딩된 문자열)
   * @return 인증 URL
   */
  String getAuthorizationUrl(ProviderType providerType, String state);

  /**
   * 인가 코드로 사용자 정보를 조회한다.
   *
   * @param providerType 제공자 타입
   * @param code 인가 코드
   * @return 사용자 정보
   */
  OAuthUserInfo getUserInfo(ProviderType providerType, String code);

  /**
   * 제공자가 설정되어 있는지 확인한다.
   *
   * @param providerType 제공자 타입
   * @return 설정되어 있으면 true
   */
  boolean isProviderConfigured(ProviderType providerType);
}