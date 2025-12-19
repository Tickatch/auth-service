package com.tickatch.auth_service.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tickatch.auth_service.auth.domain.exception.AuthErrorCode;
import com.tickatch.auth_service.auth.domain.exception.AuthException;
import com.tickatch.auth_service.auth.domain.vo.AuthStatus;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("Auth 테스트")
class AuthTest {

  private PasswordEncoder encoder;

  @BeforeEach
  void setUp() {
    encoder = new BCryptPasswordEncoder();
  }

  @Nested
  class 등록_테스트 {

    @Test
    void 유효한_정보로_Auth를_생성한다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");

      assertThat(auth.getId()).isNotNull();
      assertThat(auth.getEmail()).isEqualTo("test@example.com");
      assertThat(auth.getUserType()).isEqualTo(UserType.CUSTOMER);
      assertThat(auth.getStatus()).isEqualTo(AuthStatus.ACTIVE);
      assertThat(auth.getLoginFailCount()).isZero();
      assertThat(auth.hasPassword()).isTrue();
    }

    @Test
    void null_이메일로_생성하면_예외를_던진다() {
      assertThatThrownBy(
              () -> Auth.register(null, "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM"))
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.INVALID_EMAIL);
    }

    @Test
    void 빈_이메일로_생성하면_예외를_던진다() {
      assertThatThrownBy(() -> Auth.register("", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM"))
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.INVALID_EMAIL);
    }

    @Test
    void null_UserType으로_생성하면_예외를_던진다() {
      assertThatThrownBy(
              () -> Auth.register("test@example.com", "Pass123!", null, encoder, "SYSTEM"))
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.INVALID_USER_TYPE);
    }
  }

  @Nested
  class 소셜_로그인으로_회원가입_테스트 {

    @Test
    void 소셜_로그인으로_Auth를_생성한다() {
      Auth auth =
          Auth.registerWithOAuth(
              "test@example.com",
              "Pass123!",
              UserType.CUSTOMER,
              ProviderType.KAKAO,
              "kakao123",
              encoder,
              "SYSTEM");

      assertThat(auth.getId()).isNotNull();
      assertThat(auth.getEmail()).isEqualTo("test@example.com");
      assertThat(auth.hasPassword()).isTrue();
      assertThat(auth.hasProvider(ProviderType.KAKAO)).isTrue();
      assertThat(auth.getProviders()).hasSize(1);
    }

    @Test
    void 소셜_로그인으로_가입해도_비밀번호로_로그인_가능하다() {
      Auth auth =
          Auth.registerWithOAuth(
              "test@example.com",
              "Pass123!",
              UserType.CUSTOMER,
              ProviderType.KAKAO,
              "kakao123",
              encoder,
              "SYSTEM");

      assertThat(auth.matchesPassword("Pass123!", encoder)).isTrue();
    }

    @Test
    void SELLER는_소셜_로그인으로_가입할_수_없다() {
      assertThatThrownBy(
              () ->
                  Auth.registerWithOAuth(
                      "seller@example.com",
                      "Pass123!",
                      UserType.SELLER,
                      ProviderType.KAKAO,
                      "kakao123",
                      encoder,
                      "SYSTEM"))
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.OAUTH_NOT_ALLOWED_FOR_USER_TYPE);
    }

    @Test
    void ADMIN은_소셜_로그인으로_가입할_수_없다() {
      assertThatThrownBy(
              () ->
                  Auth.registerWithOAuth(
                      "admin@example.com",
                      "Pass123!",
                      UserType.ADMIN,
                      ProviderType.GOOGLE,
                      "google123",
                      encoder,
                      "SYSTEM"))
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.OAUTH_NOT_ALLOWED_FOR_USER_TYPE);
    }
  }

  @Nested
  class 로그인_테스트 {

    @Test
    void 로그인_성공_시_실패_횟수가_초기화된다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");

      auth.recordLoginSuccess();

      assertThat(auth.getLoginFailCount()).isZero();
      assertThat(auth.getLastLoginAt()).isNotNull();
    }

    @Test
    void 잠금_상태에서_로그인_성공_기록_시_예외를_던진다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      auth.lock();

      assertThatThrownBy(auth::recordLoginSuccess)
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.ACCOUNT_LOCKED);
    }

    @Test
    void 로그인_실패_시_실패_횟수가_증가한다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");

      auth.recordLoginFailure();

      assertThat(auth.getLoginFailCount()).isEqualTo(1);
    }

    @Test
    void 로그인_5회_실패_시_계정이_잠금된다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");

      for (int i = 0; i < 5; i++) {
        auth.recordLoginFailure();
      }

      assertThat(auth.getStatus()).isEqualTo(AuthStatus.LOCKED);
    }

    @Test
    void 비밀번호가_일치하면_true를_반환한다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");

      assertThat(auth.matchesPassword("Pass123!", encoder)).isTrue();
      assertThat(auth.matchesPassword("Wrong123!", encoder)).isFalse();
    }
  }

  @Nested
  class 비밀번호_변경_테스트 {

    @Test
    void 비밀번호를_변경한다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");

      auth.changePassword("NewPass1!", encoder, "SYSTEM");

      assertThat(auth.matchesPassword("NewPass1!", encoder)).isTrue();
      assertThat(auth.matchesPassword("Pass123!", encoder)).isFalse();
    }

    @Test
    void 잠금_상태에서_비밀번호_변경_시_예외를_던진다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      auth.lock();

      assertThatThrownBy(() -> auth.changePassword("NewPass1!", encoder, "SYSTEM"))
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.ACCOUNT_LOCKED);
    }
  }

  @Nested
  class 비밀번호_초기화_테스트 {

    @Test
    void 비밀번호_초기화_시_잠금이_해제된다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      for (int i = 0; i < 5; i++) {
        auth.recordLoginFailure();
      }
      assertThat(auth.getStatus()).isEqualTo(AuthStatus.LOCKED);

      auth.resetPassword("NewPass1!", encoder, "SYSTEM");

      assertThat(auth.getStatus()).isEqualTo(AuthStatus.ACTIVE);
      assertThat(auth.getLoginFailCount()).isZero();
      assertThat(auth.matchesPassword("NewPass1!", encoder)).isTrue();
    }

    @Test
    void 탈퇴_상태에서_비밀번호_초기화_시_예외를_던진다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      auth.withdraw("SYSTEM");

      assertThatThrownBy(() -> auth.resetPassword("NewPass1!", encoder, "SYSTEM"))
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.ACCOUNT_WITHDRAWN);
    }
  }

  @Nested
  class 계정_잠금_및_해제 {

    @Test
    void 계정을_잠금한다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");

      auth.lock();

      assertThat(auth.getStatus()).isEqualTo(AuthStatus.LOCKED);
    }

    @Test
    void 계정_잠금을_해제한다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      auth.lock();

      auth.unlock();

      assertThat(auth.getStatus()).isEqualTo(AuthStatus.ACTIVE);
      assertThat(auth.getLoginFailCount()).isZero();
    }

    @Test
    void 잠금되지_않은_계정은_해제해도_변화_없다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");

      auth.unlock();

      assertThat(auth.getStatus()).isEqualTo(AuthStatus.ACTIVE);
    }
  }

  @Nested
  class 회원_탈퇴_테스트 {

    @Test
    void 회원을_탈퇴_처리한다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");

      auth.withdraw("SYSTEM");

      assertThat(auth.getStatus()).isEqualTo(AuthStatus.WITHDRAWN);
    }

    @Test
    void 이미_탈퇴한_계정_탈퇴_시_예외를_던진다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      auth.withdraw("SYSTEM");

      assertThatThrownBy(() -> auth.withdraw("SYSTEM"))
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.ALREADY_WITHDRAWN);
    }
  }

  @Nested
  class 소셜_로그인_연동_테스트 {

    @Test
    void 소셜_로그인을_연동한다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");

      auth.connectProvider(ProviderType.KAKAO, "kakao123");

      assertThat(auth.hasProvider(ProviderType.KAKAO)).isTrue();
      assertThat(auth.getProviders()).hasSize(1);
    }

    @Test
    void 이미_연동된_제공자_연동_시_예외를_던진다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      auth.connectProvider(ProviderType.KAKAO, "kakao123");

      assertThatThrownBy(() -> auth.connectProvider(ProviderType.KAKAO, "kakao456"))
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.PROVIDER_ALREADY_CONNECTED);
    }

    @Test
    void SELLER는_소셜_로그인을_연동할_수_없다() {
      Auth auth =
          Auth.register("seller@example.com", "Pass123!", UserType.SELLER, encoder, "SYSTEM");

      assertThatThrownBy(() -> auth.connectProvider(ProviderType.KAKAO, "kakao123"))
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.OAUTH_NOT_ALLOWED_FOR_USER_TYPE);
    }

    @Test
    void ADMIN은_소셜_로그인을_연동할_수_없다() {
      Auth auth = Auth.register("admin@example.com", "Pass123!", UserType.ADMIN, encoder, "SYSTEM");

      assertThatThrownBy(() -> auth.connectProvider(ProviderType.GOOGLE, "google123"))
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.OAUTH_NOT_ALLOWED_FOR_USER_TYPE);
    }

    @Test
    void 소셜_로그인_연동을_해제한다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      auth.connectProvider(ProviderType.KAKAO, "kakao123");

      auth.disconnectProvider(ProviderType.KAKAO);

      assertThat(auth.hasProvider(ProviderType.KAKAO)).isFalse();
      assertThat(auth.getProviders()).isEmpty();
    }

    @Test
    void 모든_소셜_연동을_해제해도_비밀번호로_로그인_가능하다() {
      Auth auth =
          Auth.registerWithOAuth(
              "test@example.com",
              "Pass123!",
              UserType.CUSTOMER,
              ProviderType.KAKAO,
              "kakao123",
              encoder,
              "SYSTEM");

      auth.disconnectProvider(ProviderType.KAKAO);

      assertThat(auth.getProviders()).isEmpty();
      assertThat(auth.matchesPassword("Pass123!", encoder)).isTrue();
    }

    @Test
    void 여러_제공자를_연동할_수_있다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");

      auth.connectProvider(ProviderType.KAKAO, "kakao123");
      auth.connectProvider(ProviderType.NAVER, "naver123");
      auth.connectProvider(ProviderType.GOOGLE, "google123");

      assertThat(auth.getProviders()).hasSize(3);
      assertThat(auth.hasProvider(ProviderType.KAKAO)).isTrue();
      assertThat(auth.hasProvider(ProviderType.NAVER)).isTrue();
      assertThat(auth.hasProvider(ProviderType.GOOGLE)).isTrue();
    }
  }

  @Nested
  class 동등성_테스트 {

    @Test
    void 동일한_ID를_가진_Auth는_같다() {
      Auth auth1 =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      Auth auth2 = auth1;

      assertThat(auth1).isEqualTo(auth2);
    }

    @Test
    void 다른_ID를_가진_Auth는_다르다() {
      Auth auth1 =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      Auth auth2 =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");

      assertThat(auth1).isNotEqualTo(auth2);
    }
  }
}
