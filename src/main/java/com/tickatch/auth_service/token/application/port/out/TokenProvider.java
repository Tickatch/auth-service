package com.tickatch.auth_service.token.application.port.out;

import com.tickatch.auth_service.auth.domain.vo.UserType;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * JWT 토큰 생성 및 검증을 위한 아웃바운드 포트.
 *
 * <p>Application 계층에서 JWT 생성/검증이 필요할 때 이 인터페이스를 통해 접근한다. 실제 구현은 Infrastructure
 * 계층의 JwtTokenProvider가 담당한다.
 *
 * <p>RS256 알고리즘을 사용하여:
 * <ul>
 *   <li>서명: Private Key (Auth Service만 보유)
 *   <li>검증: Public Key (외부 공개 가능)
 * </ul>
 *
 * @author Tickatch
 * @since 1.0.0
 */
public interface TokenProvider {

  /**
   * Access Token을 생성한다.
   *
   * @param authId 인증 ID (= userId)
   * @param userType 사용자 유형
   * @return 생성된 Access Token
   */
  String generateAccessToken(UUID authId, UserType userType);

  /**
   * Refresh Token 값을 생성한다.
   *
   * <p>UUID 기반의 랜덤 문자열을 생성한다. Refresh Token은 DB에 저장되므로 JWT 형식이 아니어도 된다.
   *
   * @return 생성된 Refresh Token 값
   */
  String generateRefreshTokenValue();

  /**
   * 토큰의 유효성을 검증한다.
   *
   * @param token 검증할 토큰
   * @return 유효하면 true
   */
  boolean validateToken(String token);

  /**
   * 토큰에서 Auth ID를 추출한다.
   *
   * @param token JWT 토큰
   * @return Auth ID
   */
  UUID extractAuthId(String token);

  /**
   * 토큰에서 사용자 유형을 추출한다.
   *
   * @param token JWT 토큰
   * @return 사용자 유형
   */
  UserType extractUserType(String token);

  /**
   * JWT 서명 검증용 Public Key를 반환한다.
   *
   * <p>Gateway나 다른 서비스에서 토큰 검증에 사용한다. Public Key는 공개되어도 안전하다.
   *
   * @return RSA Public Key
   */
  RSAPublicKey getPublicKey();

  /**
   * 키 ID를 반환한다.
   *
   * <p>JWKS에서 키를 식별하는 데 사용된다. 키 로테이션 시 버전 관리에 활용한다.
   *
   * @return Key ID
   */
  String getKeyId();

  /**
   * Access Token 만료 시간을 초 단위로 반환한다.
   *
   * @return 만료 시간 (초)
   */
  long getAccessTokenExpirationSeconds();
}