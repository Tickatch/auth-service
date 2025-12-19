package com.tickatch.auth_service.auth.infrastructure.oauth;

import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OAuth 설정 프로퍼티.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

  private Map<String, Provider> providers = new HashMap<>();
  private String frontendRedirectUrl;

  @PostConstruct
  public void init() {
    // 기본값 설정
    providers.computeIfAbsent("kakao", k -> createKakaoDefaults());
    providers.computeIfAbsent("naver", k -> createNaverDefaults());
    providers.computeIfAbsent("google", k -> createGoogleDefaults());
  }

  /**
   * 제공자별 설정을 가져온다.
   *
   * @param providerType 제공자 타입
   * @return Provider 설정
   */
  public Provider getProvider(ProviderType providerType) {
    return providers.get(providerType.name().toLowerCase());
  }

  private Provider createKakaoDefaults() {
    Provider provider = new Provider();
    provider.setAuthorizationUri("https://kauth.kakao.com/oauth/authorize");
    provider.setTokenUri("https://kauth.kakao.com/oauth/token");
    provider.setUserInfoUri("https://kapi.kakao.com/v2/user/me");
    provider.setScope("profile_nickname,account_email");
    return provider;
  }

  private Provider createNaverDefaults() {
    Provider provider = new Provider();
    provider.setAuthorizationUri("https://nid.naver.com/oauth2.0/authorize");
    provider.setTokenUri("https://nid.naver.com/oauth2.0/token");
    provider.setUserInfoUri("https://openapi.naver.com/v1/nid/me");
    provider.setScope("name,email");
    return provider;
  }

  private Provider createGoogleDefaults() {
    Provider provider = new Provider();
    provider.setAuthorizationUri("https://accounts.google.com/o/oauth2/v2/auth");
    provider.setTokenUri("https://oauth2.googleapis.com/token");
    provider.setUserInfoUri("https://www.googleapis.com/oauth2/v2/userinfo");
    provider.setScope("openid,email,profile");
    return provider;
  }

  /** 개별 OAuth 제공자 설정. */
  @Getter
  @Setter
  public static class Provider {

    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String authorizationUri;
    private String tokenUri;
    private String userInfoUri;
    private String scope;

    public boolean isConfigured() {
      return clientId != null
          && !clientId.isBlank()
          && clientSecret != null
          && !clientSecret.isBlank();
    }
  }
}
