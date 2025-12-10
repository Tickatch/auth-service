package com.tickatch.auth_service.auth.application.port.out;

import com.tickatch.auth_service.auth.domain.vo.UserType;
import com.tickatch.auth_service.token.application.service.command.dto.TokenResult;
import java.util.UUID;

/**
 * 토큰 서비스 호출을 위한 아웃바운드 포트.
 *
 * <p>Auth 도메인에서 Token 도메인의 기능이 필요할 때 이 인터페이스를 통해 접근한다. 도메인 간 직접 의존을 피하고
 * 느슨한 결합을 유지한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
public interface TokenPort {

  /**
   * 토큰을 발급한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   * @param deviceInfo 디바이스 정보
   * @param rememberMe 로그인 유지 여부
   * @return 발급된 토큰 정보
   */
  TokenResult issueTokens(UUID authId, UserType userType, String deviceInfo, boolean rememberMe);

  /**
   * Refresh Token으로 새 토큰을 발급한다.
   *
   * @param refreshToken Refresh Token 값
   * @param userType 사용자 유형 (Auth에서 조회한 값)
   * @return 갱신된 토큰 정보
   */
  TokenResult refreshTokens(String refreshToken, UserType userType);

  /**
   * Refresh Token의 Auth ID를 조회한다.
   *
   * <p>토큰 갱신 시 Auth 정보 조회에 사용한다.
   *
   * @param refreshToken Refresh Token 값
   * @return Auth ID
   */
  UUID getAuthIdFromRefreshToken(String refreshToken);

  /**
   * 특정 Refresh Token을 폐기한다.
   *
   * @param refreshToken Refresh Token 값
   */
  void revokeToken(String refreshToken);

  /**
   * 사용자의 모든 토큰을 폐기한다 (전체 로그아웃).
   *
   * @param authId Auth ID
   */
  void revokeAllTokens(UUID authId);

  /**
   * 사용자의 모든 토큰을 삭제한다 (회원탈퇴).
   *
   * @param authId Auth ID
   */
  void deleteAllTokens(UUID authId);
}