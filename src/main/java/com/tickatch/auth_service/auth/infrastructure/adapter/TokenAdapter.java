package com.tickatch.auth_service.auth.infrastructure.adapter;

import com.tickatch.auth_service.auth.application.port.out.TokenPort;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import com.tickatch.auth_service.token.application.port.out.TokenProvider;
import com.tickatch.auth_service.token.application.service.command.TokenCommandService;
import com.tickatch.auth_service.token.application.service.command.dto.IssueTokenCommand;
import com.tickatch.auth_service.token.application.service.command.dto.TokenResult;
import com.tickatch.auth_service.token.domain.RefreshToken;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * TokenPort 구현체.
 *
 * <p>Auth 도메인에서 Token 도메인 기능을 호출할 때 사용되는 어댑터이다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class TokenAdapter implements TokenPort {

  private final TokenCommandService tokenCommandService;
  private final TokenProvider tokenProvider;

  @Override
  public TokenResult issueTokens(UUID authId, UserType userType, String deviceInfo, boolean rememberMe) {
    IssueTokenCommand command = IssueTokenCommand.of(authId, userType, deviceInfo, rememberMe);
    return tokenCommandService.issueTokens(command);
  }

  @Override
  public TokenResult refreshTokens(String refreshTokenValue, UserType userType) {
    // 1. Refresh Token 검증 및 조회
    RefreshToken refreshToken = tokenCommandService.validateAndGetRefreshToken(refreshTokenValue);

    // 2. 새 Access Token 생성
    String newAccessToken = tokenProvider.generateAccessToken(refreshToken.getAuthId(), userType);

    // 3. Refresh Token Rotation
    String newRefreshTokenValue = tokenCommandService.rotateRefreshToken(refreshToken);

    // 4. Access Token 만료 시간 계산
    LocalDateTime accessTokenExpiresAt = LocalDateTime.now()
        .plusSeconds(tokenProvider.getAccessTokenExpirationSeconds());

    return TokenResult.of(
        newAccessToken,
        newRefreshTokenValue,
        accessTokenExpiresAt,
        refreshToken.getExpiresAt()
    );
  }

  @Override
  public UUID getAuthIdFromRefreshToken(String refreshTokenValue) {
    RefreshToken refreshToken = tokenCommandService.validateAndGetRefreshToken(refreshTokenValue);
    return refreshToken.getAuthId();
  }

  @Override
  public void revokeToken(String refreshToken) {
    tokenCommandService.revokeToken(refreshToken);
  }

  @Override
  public void revokeAllTokens(UUID authId) {
    tokenCommandService.revokeAllByAuthId(authId);
  }

  @Override
  public void deleteAllTokens(UUID authId) {
    tokenCommandService.deleteAllByAuthId(authId);
  }
}