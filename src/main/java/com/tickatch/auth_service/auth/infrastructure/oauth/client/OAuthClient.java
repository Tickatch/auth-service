package com.tickatch.auth_service.auth.infrastructure.oauth.client;

import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.infrastructure.oauth.dto.OAuthUserInfo;

/**
 * OAuth 클라이언트 인터페이스.
 *
 * @author Tickatch
 * @since 1.0.0
 */
public interface OAuthClient {

  /** 지원하는 제공자 타입을 반환한다. */
  ProviderType getProviderType();

  /**
   * 인증 URL을 생성한다.
   *
   * @param state 상태값
   * @return 인증 URL
   */
  String getAuthorizationUrl(String state);

  /**
   * 인가 코드로 사용자 정보를 조회한다.
   *
   * @param code 인가 코드
   * @return 사용자 정보
   */
  OAuthUserInfo getUserInfo(String code);

  /** 설정이 완료되어 있는지 확인한다. */
  boolean isConfigured();
}
