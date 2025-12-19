package com.tickatch.auth_service.auth.infrastructure.oauth.client;

import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.infrastructure.oauth.OAuthProperties;
import com.tickatch.auth_service.auth.infrastructure.oauth.dto.OAuthUserInfo;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

/**
 * 구글 OAuth 클라이언트.
 *
 * <p>구글 로그인 API를 사용하여 인증을 처리한다.
 *
 * <p>구글 사용자 정보 응답 형식:
 *
 * <pre>
 * {
 *   "id": "123456789",
 *   "email": "user@gmail.com",
 *   "verified_email": true,
 *   "name": "John Doe",
 *   "given_name": "John",
 *   "family_name": "Doe",
 *   "picture": "http://..."
 * }
 * </pre>
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Slf4j
public class GoogleOAuthClient extends AbstractOAuthClient {

  public GoogleOAuthClient(OAuthProperties.Provider providerConfig, RestTemplate restTemplate) {
    super(providerConfig, restTemplate);
  }

  @Override
  public ProviderType getProviderType() {
    return ProviderType.GOOGLE;
  }

  @Override
  protected OAuthUserInfo parseUserInfo(Map<String, Object> userInfoMap) {
    String providerUserId = (String) userInfoMap.get("id");
    String email = (String) userInfoMap.get("email");
    String name = (String) userInfoMap.get("name");
    String profileImage = (String) userInfoMap.get("picture");

    log.debug("구글 사용자 정보 파싱 완료 - providerUserId: {}, email: {}", providerUserId, email);

    return OAuthUserInfo.builder()
        .providerType(ProviderType.GOOGLE)
        .providerUserId(providerUserId)
        .email(email)
        .name(name)
        .profileImage(profileImage)
        .build();
  }
}
