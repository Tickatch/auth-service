package com.tickatch.auth_service.token.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.tickatch.auth_service.token.application.service.query.dto.TokenInfo;
import com.tickatch.auth_service.token.domain.RefreshToken;
import com.tickatch.auth_service.token.domain.RefreshTokenRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("TokenQueryService 테스트")
@ExtendWith(MockitoExtension.class)
class TokenQueryServiceTest {

  @InjectMocks private TokenQueryService tokenQueryService;

  @Mock private RefreshTokenRepository refreshTokenRepository;

  private RefreshToken createRefreshToken(UUID authId, String tokenValue) {
    return RefreshToken.create(authId, tokenValue, "device-info", false);
  }

  @Nested
  class findById_테스트 {

    @Test
    void 토큰_ID로_조회를_성공한다() {
      UUID authId = UUID.randomUUID();
      RefreshToken refreshToken = createRefreshToken(authId, "token-value");
      UUID tokenId = refreshToken.getId();

      given(refreshTokenRepository.findById(tokenId)).willReturn(Optional.of(refreshToken));

      Optional<TokenInfo> result = tokenQueryService.findById(tokenId);

      assertThat(result).isPresent();
      assertThat(result.get().authId()).isEqualTo(authId);
      assertThat(result.get().deviceInfo()).isEqualTo("device-info");
    }

    @Test
    void 존재하지_않으면_Optional_empty를_반환한다() {
      UUID tokenId = UUID.randomUUID();
      given(refreshTokenRepository.findById(tokenId)).willReturn(Optional.empty());

      Optional<TokenInfo> result = tokenQueryService.findById(tokenId);

      assertThat(result).isEmpty();
    }
  }

  @Nested
  class findByToken_테스트 {

    @Test
    void 토큰_값으로_조회를_성공한다() {
      UUID authId = UUID.randomUUID();
      RefreshToken refreshToken = createRefreshToken(authId, "token-value");

      given(refreshTokenRepository.findByToken("token-value"))
          .willReturn(Optional.of(refreshToken));

      Optional<TokenInfo> result = tokenQueryService.findByToken("token-value");

      assertThat(result).isPresent();
      assertThat(result.get().authId()).isEqualTo(authId);
    }
  }

  @Nested
  class findAllByAuthId_테스트 {

    @Test
    void Auth_ID로_모든_토큰을_조회한다() {
      UUID authId = UUID.randomUUID();
      RefreshToken token1 = createRefreshToken(authId, "token1");
      RefreshToken token2 = createRefreshToken(authId, "token2");

      given(refreshTokenRepository.findAllByAuthId(authId)).willReturn(List.of(token1, token2));

      List<TokenInfo> result = tokenQueryService.findAllByAuthId(authId);

      assertThat(result).hasSize(2);
    }

    @Test
    void 토큰이_없으면_빈_리스트를_반환한다() {
      UUID authId = UUID.randomUUID();
      given(refreshTokenRepository.findAllByAuthId(authId)).willReturn(List.of());

      List<TokenInfo> result = tokenQueryService.findAllByAuthId(authId);

      assertThat(result).isEmpty();
    }
  }

  @Nested
  class findAllUsableByAuthId_테스트 {

    @Test
    void 사용_가능한_토큰만_조회한다() {
      UUID authId = UUID.randomUUID();
      RefreshToken usableToken = createRefreshToken(authId, "usable-token");

      given(refreshTokenRepository.findAllUsableByAuthId(authId)).willReturn(List.of(usableToken));

      List<TokenInfo> result = tokenQueryService.findAllUsableByAuthId(authId);

      assertThat(result).hasSize(1);
      assertThat(result.get(0).revoked()).isFalse();
    }
  }

  @Nested
  class CountActiveSessionsByAuthId_테스트 {

    @Test
    void 활성_세션_수를_조회한다() {
      UUID authId = UUID.randomUUID();
      RefreshToken token1 = createRefreshToken(authId, "token1");
      RefreshToken token2 = createRefreshToken(authId, "token2");
      RefreshToken token3 = createRefreshToken(authId, "token3");

      given(refreshTokenRepository.findAllUsableByAuthId(authId))
          .willReturn(List.of(token1, token2, token3));

      long count = tokenQueryService.countActiveSessionsByAuthId(authId);

      assertThat(count).isEqualTo(3);
    }
  }
}
