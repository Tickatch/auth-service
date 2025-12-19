package com.tickatch.auth_service.token.domain.repository.dto;

import com.tickatch.auth_service.token.domain.RefreshToken;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * RefreshToken 응답 DTO.
 *
 * <p>RefreshToken 조회 시 반환되는 데이터를 담는다. 엔티티를 외부에 노출하지 않고 필요한 필드만 전달한다.
 *
 * <p>보안상 토큰 값은 마스킹 처리한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Getter
@Builder
public class RefreshTokenResponse {

  /** 토큰 ID */
  private final UUID id;

  /** Auth ID */
  private final UUID authId;

  /** 토큰 값 (마스킹 처리) */
  private final String maskedToken;

  /** 디바이스 정보 */
  private final String deviceInfo;

  /** 만료 일시 */
  private final LocalDateTime expiresAt;

  /** 생성 일시 */
  private final LocalDateTime createdAt;

  /** 폐기 여부 */
  private final boolean revoked;

  /** 로그인 유지 여부 */
  private final boolean rememberMe;

  /** 만료 여부 */
  private final boolean expired;

  /** 사용 가능 여부 */
  private final boolean usable;

  /**
   * RefreshToken 엔티티를 응답 DTO로 변환한다.
   *
   * @param refreshToken RefreshToken 엔티티
   * @return RefreshToken 응답 DTO
   */
  public static RefreshTokenResponse from(RefreshToken refreshToken) {
    return RefreshTokenResponse.builder()
        .id(refreshToken.getId())
        .authId(refreshToken.getAuthId())
        .maskedToken(maskToken(refreshToken.getToken()))
        .deviceInfo(refreshToken.getDeviceInfo())
        .expiresAt(refreshToken.getExpiresAt())
        .createdAt(refreshToken.getCreatedAt())
        .revoked(refreshToken.isRevoked())
        .rememberMe(refreshToken.isRememberMe())
        .expired(refreshToken.isExpired())
        .usable(refreshToken.isUsable())
        .build();
  }

  /**
   * 토큰 값을 마스킹 처리한다.
   *
   * <p>앞 8자리와 뒤 4자리만 표시하고 나머지는 *로 처리한다.
   *
   * @param token 원본 토큰 값
   * @return 마스킹된 토큰 값
   */
  private static String maskToken(String token) {
    if (token == null || token.length() <= 12) {
      return "****";
    }
    return token.substring(0, 8) + "****" + token.substring(token.length() - 4);
  }
}
