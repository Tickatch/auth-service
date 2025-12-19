package com.tickatch.auth_service.auth.infrastructure.adapter;

import com.tickatch.auth_service.auth.application.port.out.OAuthPort;
import com.tickatch.auth_service.auth.domain.exception.AuthErrorCode;
import com.tickatch.auth_service.auth.domain.exception.AuthException;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.infrastructure.oauth.OAuthProperties;
import com.tickatch.auth_service.auth.infrastructure.oauth.client.GoogleOAuthClient;
import com.tickatch.auth_service.auth.infrastructure.oauth.client.KakaoOAuthClient;
import com.tickatch.auth_service.auth.infrastructure.oauth.client.NaverOAuthClient;
import com.tickatch.auth_service.auth.infrastructure.oauth.client.OAuthClient;
import com.tickatch.auth_service.auth.infrastructure.oauth.dto.OAuthUserInfo;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * OAuthPort 구현체.
 *
 * <p>OAuth 제공자별 클라이언트를 관리하고 호출한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Slf4j
@Component
public class OAuthAdapter implements OAuthPort {

  private final OAuthProperties oAuthProperties;
  private final RestTemplate restTemplate;
  private final Map<ProviderType, OAuthClient> clients = new EnumMap<>(ProviderType.class);

  public OAuthAdapter(OAuthProperties oAuthProperties, RestTemplateBuilder restTemplateBuilder) {
    this.oAuthProperties = oAuthProperties;
    this.restTemplate =
        restTemplateBuilder
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .build();
  }

  @PostConstruct
  public void init() {
    // 각 제공자별 클라이언트 초기화
    OAuthProperties.Provider kakaoConfig = oAuthProperties.getProvider(ProviderType.KAKAO);
    if (kakaoConfig != null) {
      clients.put(ProviderType.KAKAO, new KakaoOAuthClient(kakaoConfig, restTemplate));
      log.info("카카오 OAuth 클라이언트 초기화 완료 - configured: {}", kakaoConfig.isConfigured());
    }

    OAuthProperties.Provider naverConfig = oAuthProperties.getProvider(ProviderType.NAVER);
    if (naverConfig != null) {
      clients.put(ProviderType.NAVER, new NaverOAuthClient(naverConfig, restTemplate));
      log.info("네이버 OAuth 클라이언트 초기화 완료 - configured: {}", naverConfig.isConfigured());
    }

    OAuthProperties.Provider googleConfig = oAuthProperties.getProvider(ProviderType.GOOGLE);
    if (googleConfig != null) {
      clients.put(ProviderType.GOOGLE, new GoogleOAuthClient(googleConfig, restTemplate));
      log.info("구글 OAuth 클라이언트 초기화 완료 - configured: {}", googleConfig.isConfigured());
    }
  }

  @Override
  public String getAuthorizationUrl(ProviderType providerType, String state) {
    OAuthClient client = getClient(providerType);
    return client.getAuthorizationUrl(state);
  }

  @Override
  public OAuthUserInfo getUserInfo(ProviderType providerType, String code) {
    OAuthClient client = getClient(providerType);
    return client.getUserInfo(code);
  }

  @Override
  public boolean isProviderConfigured(ProviderType providerType) {
    OAuthClient client = clients.get(providerType);
    return client != null && client.isConfigured();
  }

  /** 제공자별 클라이언트를 가져온다. */
  private OAuthClient getClient(ProviderType providerType) {
    OAuthClient client = clients.get(providerType);
    if (client == null || !client.isConfigured()) {
      log.error("OAuth 제공자가 설정되지 않음 - provider: {}", providerType);
      throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
    }
    return client;
  }
}
