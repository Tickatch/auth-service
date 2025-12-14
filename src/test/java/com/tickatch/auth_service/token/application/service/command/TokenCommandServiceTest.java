package com.tickatch.auth_service.token.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.tickatch.auth_service.auth.domain.vo.UserType;
import com.tickatch.auth_service.token.application.port.out.AuthPort;
import com.tickatch.auth_service.token.application.port.out.TokenProvider;
import com.tickatch.auth_service.token.application.service.command.dto.IssueTokenCommand;
import com.tickatch.auth_service.token.application.service.command.dto.RefreshTokenCommand;
import com.tickatch.auth_service.token.application.service.command.dto.TokenResult;
import com.tickatch.auth_service.token.domain.RefreshToken;
import com.tickatch.auth_service.token.domain.RefreshTokenRepository;
import com.tickatch.auth_service.token.domain.exception.TokenErrorCode;
import com.tickatch.auth_service.token.domain.exception.TokenException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("TokenCommandService 테스트")
@ExtendWith(MockitoExtension.class)
class TokenCommandServiceTest {

  @InjectMocks
  private TokenCommandService tokenCommandService;

  @Mock
  private TokenProvider tokenProvider;

  @Mock
  private RefreshTokenRepository refreshTokenRepository;

  @Mock
  private AuthPort authPort;

  @Nested
  class 토큰_발급_테스트 {

    @Test
    void 토큰_발급을_성공한다() {
      UUID authId = UUID.randomUUID();
      IssueTokenCommand command = IssueTokenCommand.of(
          authId, UserType.CUSTOMER, "device-info", false);

      given(tokenProvider.generateAccessToken(authId, UserType.CUSTOMER))
          .willReturn("access-token");
      given(tokenProvider.generateRefreshTokenValue())
          .willReturn("refresh-token-value");
      given(tokenProvider.getAccessTokenExpirationSeconds())
          .willReturn(300L);
      given(refreshTokenRepository.save(any(RefreshToken.class)))
          .willAnswer(inv -> inv.getArgument(0));

      TokenResult result = tokenCommandService.issueTokens(command);

      assertThat(result).isNotNull();
      assertThat(result.accessToken()).isEqualTo("access-token");
      assertThat(result.refreshToken()).isEqualTo("refresh-token-value");
      verify(refreshTokenRepository).save(any(RefreshToken.class));
    }
  }

  @Nested
  class 토큰_갱신_테스트 {

    @Test
    void 토큰_갱신을_성공한다() {
      UUID authId = UUID.randomUUID();
      RefreshToken refreshToken = RefreshToken.create(authId, "old-token", "device", false);

      RefreshTokenCommand command = RefreshTokenCommand.of("old-token");

      given(refreshTokenRepository.findByToken("old-token"))
          .willReturn(Optional.of(refreshToken));
      given(authPort.findUserTypeByAuthId(authId))
          .willReturn(Optional.of(UserType.CUSTOMER));
      given(tokenProvider.generateAccessToken(authId, UserType.CUSTOMER))
          .willReturn("new-access-token");
      given(tokenProvider.generateRefreshTokenValue())
          .willReturn("new-refresh-token");
      given(tokenProvider.getAccessTokenExpirationSeconds())
          .willReturn(300L);

      TokenResult result = tokenCommandService.refreshTokens(command);

      assertThat(result).isNotNull();
      assertThat(result.accessToken()).isEqualTo("new-access-token");
      assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
    }

    @Test
    void 존재하지_않는_토큰으로_갱신_시_실패한다() {
      RefreshTokenCommand command = RefreshTokenCommand.of("invalid-token");

      given(refreshTokenRepository.findByToken("invalid-token"))
          .willReturn(Optional.empty());

      assertThatThrownBy(() -> tokenCommandService.refreshTokens(command))
          .isInstanceOf(TokenException.class)
          .hasFieldOrPropertyWithValue("errorCode", TokenErrorCode.INVALID_REFRESH_TOKEN);
    }

    @Test
    void 폐기된_토큰으로_갱신_시_실패한다() {
      UUID authId = UUID.randomUUID();
      RefreshToken refreshToken = RefreshToken.create(authId, "revoked-token", "device", false);
      refreshToken.revoke();

      RefreshTokenCommand command = RefreshTokenCommand.of("revoked-token");

      given(refreshTokenRepository.findByToken("revoked-token"))
          .willReturn(Optional.of(refreshToken));

      assertThatThrownBy(() -> tokenCommandService.refreshTokens(command))
          .isInstanceOf(TokenException.class)
          .hasFieldOrPropertyWithValue("errorCode", TokenErrorCode.TOKEN_ALREADY_REVOKED);

      verify(refreshTokenRepository).revokeAllByAuthId(authId);
    }
  }

  @Nested
  class 토큰_폐기_테스트 {

    @Test
    void 토큰_폐기를_성공한다() {
      UUID authId = UUID.randomUUID();
      RefreshToken refreshToken = RefreshToken.create(authId, "token-value", "device", false);

      given(refreshTokenRepository.findByToken("token-value"))
          .willReturn(Optional.of(refreshToken));

      tokenCommandService.revokeToken("token-value");

      assertThat(refreshToken.isRevoked()).isTrue();
    }
  }

  @Nested
  class 전체_토큰_폐기_테스트 {

    @Test
    void 전체_토큰을_폐기한다() {
      UUID authId = UUID.randomUUID();
      given(refreshTokenRepository.revokeAllByAuthId(authId)).willReturn(3);

      tokenCommandService.revokeAllByAuthId(authId);

      verify(refreshTokenRepository).revokeAllByAuthId(authId);
    }
  }

  @Nested
  class 전체_토큰_삭제_테스트 {

    @Test
    void 전체_토큰_삭제를_성공한다() {
      UUID authId = UUID.randomUUID();

      tokenCommandService.deleteAllByAuthId(authId);

      verify(refreshTokenRepository).deleteAllByAuthId(authId);
    }
  }
}