package com.tickatch.auth_service.auth.infrastructure.oauth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * OAuth 상태값.
 *
 * <p>OAuth 흐름에서 state 파라미터에 담아 전달하는 정보이다. CSRF 방지 및 추가 정보 전달에 사용한다.
 *
 * <p>OAuth는 CUSTOMER 전용이므로 userType은 포함하지 않는다.
 *
 * @param nonce CSRF 방지용 난수
 * @param rememberMe 로그인 유지 여부
 * @param deviceInfo 디바이스 정보
 * @param linkAuthId 계정 연동 시 기존 Auth ID (연동이 아닌 경우 null)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OAuthState(String nonce, boolean rememberMe, String deviceInfo, UUID linkAuthId) {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /** 로그인용 상태값 생성. */
  public static OAuthState forLogin(boolean rememberMe, String deviceInfo) {
    return new OAuthState(UUID.randomUUID().toString(), rememberMe, deviceInfo, null);
  }

  /** 계정 연동용 상태값 생성. */
  public static OAuthState forLink(UUID authId, String deviceInfo) {
    return new OAuthState(UUID.randomUUID().toString(), false, deviceInfo, authId);
  }

  /** Base64 인코딩된 문자열로 변환. */
  public String encode() {
    try {
      String json = OBJECT_MAPPER.writeValueAsString(this);
      return Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to encode OAuth state", e);
    }
  }

  /** Base64 인코딩된 문자열에서 복원. */
  public static OAuthState decode(String encoded) {
    try {
      byte[] decoded = Base64.getUrlDecoder().decode(encoded);
      String json = new String(decoded, StandardCharsets.UTF_8);
      return OBJECT_MAPPER.readValue(json, OAuthState.class);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to decode OAuth state", e);
    }
  }

  /** 계정 연동 요청인지 확인. */
  public boolean isLinkRequest() {
    return linkAuthId != null;
  }
}
