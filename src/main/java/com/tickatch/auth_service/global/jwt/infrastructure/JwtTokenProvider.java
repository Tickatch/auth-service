package com.tickatch.auth_service.global.jwt.infrastructure;

import com.tickatch.auth_service.auth.domain.vo.UserType;
import com.tickatch.auth_service.token.application.port.out.TokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 생성 및 검증 구현체 (RS256).
 *
 * <p>RSA 비대칭키를 사용하여 JWT를 생성하고 검증한다.
 *
 * <ul>
 *   <li>서명: Private Key 사용 (Auth Service만 보유)
 *   <li>검증: Public Key 사용 (Gateway, 다른 서비스에 공개 가능)
 * </ul>
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider implements TokenProvider {

  private static final String CLAIM_USER_TYPE = "userType";

  private final JwtProperties jwtProperties;
  private final RsaKeyManager rsaKeyManager;

  @Override
  public String generateAccessToken(UUID authId, UserType userType) {
    Date now = new Date();
    Date expiry = new Date(now.getTime() + jwtProperties.getAccessTokenExpirationMillis());

    return Jwts.builder()
        .subject(authId.toString())
        .claim(CLAIM_USER_TYPE, userType.name())
        .issuer(jwtProperties.getIssuer())
        .issuedAt(now)
        .expiration(expiry)
        .header()
        .keyId(jwtProperties.getKeyId())
        .and()
        .signWith(rsaKeyManager.getPrivateKey())
        .compact();
  }

  @Override
  public String generateRefreshTokenValue() {
    return UUID.randomUUID().toString().replace("-", "")
        + UUID.randomUUID().toString().replace("-", "");
  }

  @Override
  public boolean validateToken(String token) {
    if (token == null || token.isBlank()) {
      return false;
    }
    try {
      Jwts.parser().verifyWith(rsaKeyManager.getPublicKey()).build().parseSignedClaims(token);
      return true;
    } catch (ExpiredJwtException e) {
      log.debug("만료된 JWT 토큰: {}", e.getMessage());
    } catch (JwtException e) {
      log.debug("유효하지 않은 JWT 토큰: {}", e.getMessage());
    }
    return false;
  }

  @Override
  public UUID extractAuthId(String token) {
    Claims claims = extractClaims(token);
    return UUID.fromString(claims.getSubject());
  }

  @Override
  public UserType extractUserType(String token) {
    Claims claims = extractClaims(token);
    String userTypeStr = claims.get(CLAIM_USER_TYPE, String.class);
    return UserType.valueOf(userTypeStr);
  }

  @Override
  public RSAPublicKey getPublicKey() {
    return rsaKeyManager.getPublicKey();
  }

  @Override
  public String getKeyId() {
    return jwtProperties.getKeyId();
  }

  @Override
  public long getAccessTokenExpirationSeconds() {
    return jwtProperties.getAccessToken().getExpiration();
  }

  /**
   * 토큰에서 Claims를 추출한다.
   *
   * @param token JWT 토큰
   * @return Claims
   */
  private Claims extractClaims(String token) {
    return Jwts.parser()
        .verifyWith(rsaKeyManager.getPublicKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
