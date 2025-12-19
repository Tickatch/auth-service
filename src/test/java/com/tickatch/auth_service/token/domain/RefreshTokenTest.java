package com.tickatch.auth_service.token.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tickatch.auth_service.token.domain.exception.TokenErrorCode;
import com.tickatch.auth_service.token.domain.exception.TokenException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RefreshToken 테스트")
class RefreshTokenTest {

  @Nested
  class 생성_메서드 {

    @Test
    void RefreshToken을_생성한다() {
      UUID authId = UUID.randomUUID();
      LocalDateTime before = LocalDateTime.now().plusMinutes(59);
      LocalDateTime after = LocalDateTime.now().plusMinutes(61);

      RefreshToken refreshToken = RefreshToken.create(authId, "token", "device", false);

      assertThat(refreshToken.getExpiresAt()).isAfter(before);
      assertThat(refreshToken.getExpiresAt()).isBefore(after);
      assertThat(refreshToken.isRememberMe()).isFalse();
    }

    @Test
    void 로그인_유지_선택_시_만료_기간은_30일이다() {
      UUID authId = UUID.randomUUID();
      LocalDateTime before = LocalDateTime.now().plusDays(29);
      LocalDateTime after = LocalDateTime.now().plusDays(31);

      RefreshToken refreshToken = RefreshToken.create(authId, "token", "device", true);

      assertThat(refreshToken.getExpiresAt()).isAfter(before);
      assertThat(refreshToken.getExpiresAt()).isBefore(after);
      assertThat(refreshToken.isRememberMe()).isTrue();
    }
  }

  @Nested
  class createWithExpiry_테스트 {

    @Test
    void 지정된_만료_시간으로_RefreshToken을_생성한다() {
      UUID authId = UUID.randomUUID();
      LocalDateTime customExpiry = LocalDateTime.now().plusDays(7);

      RefreshToken refreshToken =
          RefreshToken.createWithExpiry(authId, "token", "device", customExpiry, false);

      assertThat(refreshToken.getExpiresAt()).isEqualTo(customExpiry);
    }
  }

  @Nested
  class rotate_테스트 {

    @Test
    void 토큰을_새_값으로_교체한다() {
      RefreshToken refreshToken =
          RefreshToken.create(UUID.randomUUID(), "old-token", "device", false);
      String newToken = "new-token";

      refreshToken.rotate(newToken);

      assertThat(refreshToken.getToken()).isEqualTo(newToken);
    }

    @Test
    void 로그인_유지_미선택_시_토큰_교체하면_만료_기간이_1시간으로_갱신된다() {
      LocalDateTime shortExpiry = LocalDateTime.now().plusMinutes(30);
      RefreshToken refreshToken =
          RefreshToken.createWithExpiry(UUID.randomUUID(), "token", "device", shortExpiry, false);
      LocalDateTime before = LocalDateTime.now().plusMinutes(59);

      refreshToken.rotate("new-token");

      assertThat(refreshToken.getExpiresAt()).isAfter(before);
    }

    @Test
    void 로그인_유지_선택_시_토큰_교체하면_만료_기간이_30일로_갱신된다() {
      LocalDateTime shortExpiry = LocalDateTime.now().plusDays(1);
      RefreshToken refreshToken =
          RefreshToken.createWithExpiry(UUID.randomUUID(), "token", "device", shortExpiry, true);
      LocalDateTime before = LocalDateTime.now().plusDays(29);

      refreshToken.rotate("new-token");

      assertThat(refreshToken.getExpiresAt()).isAfter(before);
    }

    @Test
    void 폐기된_토큰은_교체할_수_없다() {
      RefreshToken refreshToken = RefreshToken.create(UUID.randomUUID(), "token", "device", false);
      refreshToken.revoke();

      assertThatThrownBy(() -> refreshToken.rotate("new-token"))
          .isInstanceOf(TokenException.class)
          .extracting(e -> ((TokenException) e).getErrorCode())
          .isEqualTo(TokenErrorCode.TOKEN_ALREADY_REVOKED);
    }

    @Test
    void 만료된_토큰은_교체할_수_없다() {
      LocalDateTime pastExpiry = LocalDateTime.now().minusDays(1);
      RefreshToken refreshToken =
          RefreshToken.createWithExpiry(UUID.randomUUID(), "token", "device", pastExpiry, false);

      assertThatThrownBy(() -> refreshToken.rotate("new-token"))
          .isInstanceOf(TokenException.class)
          .extracting(e -> ((TokenException) e).getErrorCode())
          .isEqualTo(TokenErrorCode.REFRESH_TOKEN_EXPIRED);
    }
  }

  @Nested
  class revoke_테스트 {

    @Test
    void 토큰을_폐기한다() {
      RefreshToken refreshToken = RefreshToken.create(UUID.randomUUID(), "token", "device", false);

      refreshToken.revoke();

      assertThat(refreshToken.isRevoked()).isTrue();
    }
  }

  @Nested
  class isExpired_테스트 {

    @Test
    void 만료되지_않은_토큰은_false를_반환한다() {
      RefreshToken refreshToken = RefreshToken.create(UUID.randomUUID(), "token", "device", false);

      assertThat(refreshToken.isExpired()).isFalse();
    }

    @Test
    void 만료된_토큰은_true를_반환한다() {

      LocalDateTime pastExpiry = LocalDateTime.now().minusDays(1);
      RefreshToken refreshToken =
          RefreshToken.createWithExpiry(UUID.randomUUID(), "token", "device", pastExpiry, false);

      assertThat(refreshToken.isExpired()).isTrue();
    }
  }

  @Nested
  class isUsable_테스트 {

    @Test
    void 사용_가능한_토큰은_true_를_반환한다() {
      RefreshToken refreshToken = RefreshToken.create(UUID.randomUUID(), "token", "device", false);

      assertThat(refreshToken.isUsable()).isTrue();
    }

    @Test
    void 폐기된_토큰은_false를_반환한다() {
      RefreshToken refreshToken = RefreshToken.create(UUID.randomUUID(), "token", "device", false);
      refreshToken.revoke();

      assertThat(refreshToken.isUsable()).isFalse();
    }

    @Test
    void 만료된_토큰은_false를_반환한다() {
      LocalDateTime pastExpiry = LocalDateTime.now().minusDays(1);
      RefreshToken refreshToken =
          RefreshToken.createWithExpiry(UUID.randomUUID(), "token", "device", pastExpiry, false);

      assertThat(refreshToken.isUsable()).isFalse();
    }
  }

  @Nested
  class validateUsable_테스트 {

    @Test
    void 사용_가능한_토큰은_예외를_던지지_않는다() {
      RefreshToken refreshToken = RefreshToken.create(UUID.randomUUID(), "token", "device", false);

      refreshToken.validateUsable();
    }

    @Test
    void 폐기된_토큰은_예외를_던진다() {
      RefreshToken refreshToken = RefreshToken.create(UUID.randomUUID(), "token", "device", false);
      refreshToken.revoke();

      assertThatThrownBy(refreshToken::validateUsable)
          .isInstanceOf(TokenException.class)
          .extracting(e -> ((TokenException) e).getErrorCode())
          .isEqualTo(TokenErrorCode.TOKEN_ALREADY_REVOKED);
    }

    @Test
    void 만료된_토큰은_예외를_던진다() {
      LocalDateTime pastExpiry = LocalDateTime.now().minusDays(1);
      RefreshToken refreshToken =
          RefreshToken.createWithExpiry(UUID.randomUUID(), "token", "device", pastExpiry, false);

      assertThatThrownBy(refreshToken::validateUsable)
          .isInstanceOf(TokenException.class)
          .extracting(e -> ((TokenException) e).getErrorCode())
          .isEqualTo(TokenErrorCode.REFRESH_TOKEN_EXPIRED);
    }
  }

  @Nested
  class 동등성_테스트 {

    @Test
    void 동일한_ID를_가진_RefreshToken은_같다() {
      RefreshToken token1 = RefreshToken.create(UUID.randomUUID(), "token", "device", false);
      RefreshToken token2 = token1;

      assertThat(token1).isEqualTo(token2);
      assertThat(token1.hashCode()).isEqualTo(token2.hashCode());
    }

    @Test
    void 다른_ID를_가진_RefreshToken은_다르다() {
      UUID authId = UUID.randomUUID();
      RefreshToken token1 = RefreshToken.create(authId, "token", "device", false);
      RefreshToken token2 = RefreshToken.create(authId, "token", "device", false);

      assertThat(token1).isNotEqualTo(token2);
    }
  }
}
