package com.tickatch.auth_service.auth.infrastructure.oauth.client;

import com.tickatch.auth_service.auth.domain.exception.AuthErrorCode;
import com.tickatch.auth_service.auth.domain.exception.AuthException;
import com.tickatch.auth_service.auth.infrastructure.oauth.OAuthProperties;
import com.tickatch.auth_service.auth.infrastructure.oauth.dto.OAuthTokenResponse;
import com.tickatch.auth_service.auth.infrastructure.oauth.dto.OAuthUserInfo;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * OAuth 클라이언트 추상 클래스.
 *
 * <p>공통 로직을 구현하고, 제공자별 사용자 정보 파싱은 하위 클래스에서 구현한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Slf4j
public abstract class AbstractOAuthClient implements OAuthClient {

  protected final OAuthProperties.Provider providerConfig;
  protected final RestTemplate restTemplate;

  protected AbstractOAuthClient(
      OAuthProperties.Provider providerConfig, RestTemplate restTemplate) {
    this.providerConfig = providerConfig;
    this.restTemplate = restTemplate;
  }

  @Override
  public String getAuthorizationUrl(String state) {
    return UriComponentsBuilder.fromUriString(providerConfig.getAuthorizationUri())
        .queryParam("client_id", providerConfig.getClientId())
        .queryParam("redirect_uri", providerConfig.getRedirectUri())
        .queryParam("response_type", "code")
        .queryParam("scope", providerConfig.getScope())
        .queryParam("state", state)
        .build()
        .toUriString();
  }

  @Override
  public OAuthUserInfo getUserInfo(String code) {
    // 1. 인가 코드로 액세스 토큰 발급
    OAuthTokenResponse tokenResponse = getAccessToken(code);

    // 2. 액세스 토큰으로 사용자 정보 조회
    Map<String, Object> userInfoMap = fetchUserInfo(tokenResponse.accessToken());

    // 3. 제공자별 파싱
    return parseUserInfo(userInfoMap);
  }

  @Override
  public boolean isConfigured() {
    return providerConfig != null && providerConfig.isConfigured();
  }

  /** 인가 코드로 액세스 토큰을 발급받는다. */
  protected OAuthTokenResponse getAccessToken(String code) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

    MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "authorization_code");
    params.add("client_id", providerConfig.getClientId());
    params.add("client_secret", providerConfig.getClientSecret());
    params.add("redirect_uri", providerConfig.getRedirectUri());
    params.add("code", code);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

    try {
      ResponseEntity<OAuthTokenResponse> response =
          restTemplate.postForEntity(
              providerConfig.getTokenUri(), request, OAuthTokenResponse.class);

      if (response.getBody() == null || response.getBody().accessToken() == null) {
        log.error("OAuth 토큰 응답이 비어있습니다 - provider: {}", getProviderType());
        throw new AuthException(AuthErrorCode.OAUTH_TOKEN_FAILED);
      }

      return response.getBody();
    } catch (RestClientException e) {
      log.error("OAuth 토큰 발급 실패 - provider: {}, error: {}", getProviderType(), e.getMessage());
      throw new AuthException(AuthErrorCode.OAUTH_TOKEN_FAILED);
    }
  }

  /** 액세스 토큰으로 사용자 정보를 조회한다. */
  @SuppressWarnings("unchecked")
  protected Map<String, Object> fetchUserInfo(String accessToken) {
    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(accessToken);

    HttpEntity<Void> request = new HttpEntity<>(headers);

    try {
      ResponseEntity<Map> response =
          restTemplate.exchange(
              providerConfig.getUserInfoUri(), HttpMethod.GET, request, Map.class);

      if (response.getBody() == null) {
        log.error("OAuth 사용자 정보 응답이 비어있습니다 - provider: {}", getProviderType());
        throw new AuthException(AuthErrorCode.OAUTH_USER_INFO_FAILED);
      }

      return response.getBody();
    } catch (RestClientException e) {
      log.error("OAuth 사용자 정보 조회 실패 - provider: {}, error: {}", getProviderType(), e.getMessage());
      throw new AuthException(AuthErrorCode.OAUTH_USER_INFO_FAILED);
    }
  }

  /** 제공자별 사용자 정보 파싱. 하위 클래스에서 구현한다. */
  protected abstract OAuthUserInfo parseUserInfo(Map<String, Object> userInfoMap);
}
