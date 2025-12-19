package com.tickatch.auth_service.token.application.service.command;

import com.tickatch.auth_service.token.application.port.out.AuthPort;
import com.tickatch.auth_service.token.application.port.out.TokenProvider;
import com.tickatch.auth_service.token.application.service.command.dto.IssueTokenCommand;
import com.tickatch.auth_service.token.application.service.command.dto.RefreshTokenCommand;
import com.tickatch.auth_service.token.application.service.command.dto.TokenResult;
import com.tickatch.auth_service.token.domain.RefreshToken;
import com.tickatch.auth_service.token.domain.RefreshTokenRepository;
import com.tickatch.auth_service.token.domain.exception.TokenErrorCode;
import com.tickatch.auth_service.token.domain.exception.TokenException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토큰 관련 Command 서비스.
 *
 * <p>토큰 발급, 갱신, 폐기를 담당한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TokenCommandService {

  private final TokenProvider tokenProvider;
  private final RefreshTokenRepository refreshTokenRepository;
  private final AuthPort authPort;

  /**
   * 토큰을 발급한다.
   *
   * <p>Access Token과 Refresh Token을 생성하고, Refresh Token은 DB에 저장한다.
   *
   * @param command 토큰 발급 요청
   * @return 발급된 토큰 정보
   */
  public TokenResult issueTokens(IssueTokenCommand command) {
    String accessToken = tokenProvider.generateAccessToken(command.authId(), command.userType());
    String refreshTokenValue = tokenProvider.generateRefreshTokenValue();

    RefreshToken refreshToken =
        RefreshToken.create(
            command.authId(), refreshTokenValue, command.deviceInfo(), command.rememberMe());

    refreshTokenRepository.save(refreshToken);

    log.info("토큰 발급 완료 - authId: {}, deviceInfo: {}", command.authId(), command.deviceInfo());

    return TokenResult.of(
        accessToken, refreshTokenValue, calculateAccessTokenExpiry(), refreshToken.getExpiresAt());
  }

  /**
   * 토큰을 갱신한다 (Rotation).
   *
   * <p>기존 Refresh Token을 검증하고, 새로운 Access Token과 Refresh Token을 발급한다. 기존 Refresh Token은 새 값으로 교체된다.
   *
   * @param command 토큰 갱신 요청
   * @return 갱신된 토큰 정보
   * @throws TokenException 토큰이 유효하지 않은 경우
   */
  public TokenResult refreshTokens(RefreshTokenCommand command) {
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByToken(command.refreshToken())
            .orElseThrow(() -> new TokenException(TokenErrorCode.INVALID_REFRESH_TOKEN));

    // 이미 폐기된 토큰 사용 시도 감지 (탈취 가능성)
    if (refreshToken.isRevoked()) {
      log.warn("폐기된 토큰 재사용 시도 감지 - authId: {}", refreshToken.getAuthId());
      // 보안: 해당 사용자의 모든 토큰 폐기
      refreshTokenRepository.revokeAllByAuthId(refreshToken.getAuthId());
      throw new TokenException(TokenErrorCode.TOKEN_ALREADY_REVOKED);
    }

    // 만료된 토큰 검증
    refreshToken.validateUsable();

    // Auth에서 userType 조회
    var userType =
        authPort
            .findUserTypeByAuthId(refreshToken.getAuthId())
            .orElseThrow(() -> new TokenException(TokenErrorCode.AUTH_NOT_FOUND));

    // 새 Access Token 생성
    String newAccessToken = tokenProvider.generateAccessToken(refreshToken.getAuthId(), userType);

    // 새 토큰 값으로 Rotation
    String newRefreshTokenValue = tokenProvider.generateRefreshTokenValue();
    refreshToken.rotate(newRefreshTokenValue);

    log.info("토큰 갱신 완료 - authId: {}", refreshToken.getAuthId());

    return TokenResult.of(
        newAccessToken,
        newRefreshTokenValue,
        calculateAccessTokenExpiry(),
        refreshToken.getExpiresAt());
  }

  /**
   * Refresh Token을 검증하고 정보를 반환한다.
   *
   * <p>토큰 갱신 시 Auth 정보 조회에 사용한다.
   *
   * @param refreshTokenValue Refresh Token 값
   * @return 검증된 RefreshToken
   * @throws TokenException 토큰이 유효하지 않은 경우
   */
  public RefreshToken validateAndGetRefreshToken(String refreshTokenValue) {
    RefreshToken refreshToken =
        refreshTokenRepository
            .findByToken(refreshTokenValue)
            .orElseThrow(() -> new TokenException(TokenErrorCode.INVALID_REFRESH_TOKEN));

    if (refreshToken.isRevoked()) {
      log.warn("폐기된 토큰 재사용 시도 감지 - authId: {}", refreshToken.getAuthId());
      refreshTokenRepository.revokeAllByAuthId(refreshToken.getAuthId());
      throw new TokenException(TokenErrorCode.TOKEN_ALREADY_REVOKED);
    }

    refreshToken.validateUsable();
    return refreshToken;
  }

  /**
   * Refresh Token을 새 값으로 갱신한다 (Rotation).
   *
   * @param refreshToken 갱신할 RefreshToken
   * @return 새 Refresh Token 값
   */
  public String rotateRefreshToken(RefreshToken refreshToken) {
    String newRefreshTokenValue = tokenProvider.generateRefreshTokenValue();
    refreshToken.rotate(newRefreshTokenValue);
    return newRefreshTokenValue;
  }

  /**
   * 특정 Refresh Token을 폐기한다.
   *
   * @param refreshTokenValue Refresh Token 값
   */
  public void revokeToken(String refreshTokenValue) {
    refreshTokenRepository
        .findByToken(refreshTokenValue)
        .ifPresent(
            token -> {
              token.revoke();
              log.info("토큰 폐기 완료 - tokenId: {}", token.getId());
            });
  }

  /**
   * 사용자의 모든 Refresh Token을 폐기한다 (로그아웃).
   *
   * @param authId Auth ID
   */
  public void revokeAllByAuthId(UUID authId) {
    int revokedCount = refreshTokenRepository.revokeAllByAuthId(authId);
    log.info("모든 토큰 폐기 완료 - authId: {}, count: {}", authId, revokedCount);
  }

  /**
   * 사용자의 모든 Refresh Token을 삭제한다 (회원탈퇴).
   *
   * @param authId Auth ID
   */
  public void deleteAllByAuthId(UUID authId) {
    refreshTokenRepository.deleteAllByAuthId(authId);
    log.info("모든 토큰 삭제 완료 - authId: {}", authId);
  }

  /** Access Token 만료 시간을 계산한다. */
  private LocalDateTime calculateAccessTokenExpiry() {
    return LocalDateTime.now().plusSeconds(tokenProvider.getAccessTokenExpirationSeconds());
  }
}
