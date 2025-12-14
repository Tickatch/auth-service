package com.tickatch.auth_service.auth.infrastructure.oauth.client;

import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.infrastructure.oauth.OAuthProperties;
import com.tickatch.auth_service.auth.infrastructure.oauth.dto.OAuthUserInfo;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

/**
 * 카카오 OAuth 클라이언트.
 *
 * <p>카카오 로그인 API를 사용하여 인증을 처리한다.
 *
 * <p>카카오 사용자 정보 응답 형식:
 * <pre>
 * {
 *   "id": 123456789,
 *   "kakao_account": {
 *     "email": "user@example.com",
 *     "profile": {
 *       "nickname": "홍길동",
 *       "profile_image_url": "http://..."
 *     }
 *   }
 * }
 * </pre>
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Slf4j
public class KakaoOAuthClient extends AbstractOAuthClient {

  public KakaoOAuthClient(OAuthProperties.Provider providerConfig, RestTemplate restTemplate) {
    super(providerConfig, restTemplate);
  }

  @Override
  public ProviderType getProviderType() {
    return ProviderType.KAKAO;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected OAuthUserInfo parseUserInfo(Map<String, Object> userInfoMap) {
    String providerUserId = String.valueOf(userInfoMap.get("id"));

    Map<String, Object> kakaoAccount = (Map<String, Object>) userInfoMap.get("kakao_account");
    String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

    Map<String, Object> profile = kakaoAccount != null
        ? (Map<String, Object>) kakaoAccount.get("profile")
        : null;
    String nickname = profile != null ? (String) profile.get("nickname") : null;
    String profileImage = profile != null ? (String) profile.get("profile_image_url") : null;

    log.debug("카카오 사용자 정보 파싱 완료 - providerUserId: {}, email: {}", providerUserId, email);

    return OAuthUserInfo.builder()
        .providerType(ProviderType.KAKAO)
        .providerUserId(providerUserId)
        .email(email)
        .name(nickname)
        .profileImage(profileImage)
        .build();
  }
}