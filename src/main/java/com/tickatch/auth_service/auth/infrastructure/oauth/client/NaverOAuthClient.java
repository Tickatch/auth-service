package com.tickatch.auth_service.auth.infrastructure.oauth.client;

import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.infrastructure.oauth.OAuthProperties;
import com.tickatch.auth_service.auth.infrastructure.oauth.dto.OAuthUserInfo;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

/**
 * 네이버 OAuth 클라이언트.
 *
 * <p>네이버 로그인 API를 사용하여 인증을 처리한다.
 *
 * <p>네이버 사용자 정보 응답 형식:
 *
 * <pre>
 * {
 *   "resultcode": "00",
 *   "message": "success",
 *   "response": {
 *     "id": "abc123",
 *     "email": "user@example.com",
 *     "name": "홍길동",
 *     "profile_image": "http://..."
 *   }
 * }
 * </pre>
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Slf4j
public class NaverOAuthClient extends AbstractOAuthClient {

  public NaverOAuthClient(OAuthProperties.Provider providerConfig, RestTemplate restTemplate) {
    super(providerConfig, restTemplate);
  }

  @Override
  public ProviderType getProviderType() {
    return ProviderType.NAVER;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected OAuthUserInfo parseUserInfo(Map<String, Object> userInfoMap) {
    Map<String, Object> response = (Map<String, Object>) userInfoMap.get("response");

    String providerUserId = (String) response.get("id");
    String email = (String) response.get("email");
    String name = (String) response.get("name");
    String profileImage = (String) response.get("profile_image");

    log.debug("네이버 사용자 정보 파싱 완료 - providerUserId: {}, email: {}", providerUserId, email);

    return OAuthUserInfo.builder()
        .providerType(ProviderType.NAVER)
        .providerUserId(providerUserId)
        .email(email)
        .name(name)
        .profileImage(profileImage)
        .build();
  }
}
