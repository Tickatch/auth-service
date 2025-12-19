package com.tickatch.auth_service.auth.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 소셜 로그인 제공자 유형.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum ProviderType {

  /** 카카오 */
  KAKAO("카카오"),

  /** 네이버 */
  NAVER("네이버"),

  /** 구글 */
  GOOGLE("구글");

  /** 제공자 설명 */
  private final String description;
}
