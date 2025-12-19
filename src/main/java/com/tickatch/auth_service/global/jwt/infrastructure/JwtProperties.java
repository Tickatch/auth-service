package com.tickatch.auth_service.global.jwt.infrastructure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 설정 프로퍼티.
 *
 * <p>application.yml의 jwt.* 설정을 바인딩한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

  /** JWT 발급자. */
  private String issuer = "tickatch";

  /** 키 ID (키 로테이션 시 버전 관리용). */
  private String keyId = "tickatch-auth-key-1";

  /** 키 저장 디렉토리 경로. */
  private String keyDirectory = "data/keys";

  /** Private Key 파일 경로 (선택). */
  private String privateKeyPath;

  /** Public Key 파일 경로 (선택). */
  private String publicKeyPath;

  /** Private Key 값 (Base64 인코딩, 환경변수 주입용). */
  private String privateKey;

  /** Public Key 값 (Base64 인코딩, 환경변수 주입용). */
  private String publicKey;

  /** Access Token 설정. */
  private AccessToken accessToken = new AccessToken();

  @Getter
  @Setter
  public static class AccessToken {
    /** 만료 시간 (초 단위, 기본 5분). */
    private long expiration = 300;
  }

  /**
   * Access Token 만료 시간을 밀리초로 반환한다.
   *
   * @return 만료 시간 (밀리초)
   */
  public long getAccessTokenExpirationMillis() {
    return accessToken.getExpiration() * 1000;
  }

  /**
   * 환경변수로 Private Key가 제공되었는지 확인한다.
   *
   * @return Private Key가 환경변수로 제공되면 true
   */
  public boolean hasPrivateKeyValue() {
    return privateKey != null && !privateKey.isBlank();
  }

  /**
   * 환경변수로 Public Key가 제공되었는지 확인한다.
   *
   * @return Public Key가 환경변수로 제공되면 true
   */
  public boolean hasPublicKeyValue() {
    return publicKey != null && !publicKey.isBlank();
  }
}
