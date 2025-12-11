package com.tickatch.auth_service.token.application.service.query.dto;

import com.tickatch.auth_service.token.domain.RefreshToken;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 토큰 정보 조회 결과.
 *
 * @param id 토큰 ID
 * @param authId Auth ID
 * @param deviceInfo 디바이스 정보
 * @param createdAt 생성 일시
 * @param expiresAt 만료 일시
 * @param revoked 폐기 여부
 * @param rememberMe 로그인 유지 여부
 * @param usable 사용 가능 여부
 */
public record TokenInfo(
    UUID id,
    UUID authId,
    String deviceInfo,
    LocalDateTime createdAt,
    LocalDateTime expiresAt,
    boolean revoked,
    boolean rememberMe,
    boolean usable
) {

  public static TokenInfo from(RefreshToken token) {
    return new TokenInfo(
        token.getId(),
        token.getAuthId(),
        token.getDeviceInfo(),
        token.getCreatedAt(),
        token.getExpiresAt(),
        token.isRevoked(),
        token.isRememberMe(),
        token.isUsable()
    );
  }
}