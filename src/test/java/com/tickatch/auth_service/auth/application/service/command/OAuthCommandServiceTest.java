package com.tickatch.auth_service.auth.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.tickatch.auth_service.auth.application.port.out.OAuthPort;
import com.tickatch.auth_service.auth.application.port.out.TokenPort;
import com.tickatch.auth_service.auth.application.service.command.dto.LoginResult;
import com.tickatch.auth_service.auth.domain.Auth;
import com.tickatch.auth_service.auth.domain.AuthRepository;
import com.tickatch.auth_service.auth.domain.exception.AuthErrorCode;
import com.tickatch.auth_service.auth.domain.exception.AuthException;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import com.tickatch.auth_service.auth.infrastructure.oauth.dto.OAuthState;
import com.tickatch.auth_service.auth.infrastructure.oauth.dto.OAuthUserInfo;
import com.tickatch.auth_service.token.application.service.command.dto.TokenResult;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("OAuthCommandService 테스트")
@ExtendWith(MockitoExtension.class)
class OAuthCommandServiceTest {
  @InjectMocks
  private OAuthCommandService oAuthCommandService;

  @Mock
  private AuthRepository authRepository;

  @Mock
  private OAuthPort oAuthPort;

  @Mock
  private TokenPort tokenPort;

  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    passwordEncoder = new BCryptPasswordEncoder();
    oAuthCommandService = new OAuthCommandService(
        authRepository, oAuthPort, tokenPort, passwordEncoder);
  }

  private TokenResult createTokenResult() {
    return TokenResult.of(
        "access-token",
        "refresh-token",
        LocalDateTime.now().plusMinutes(5),
        LocalDateTime.now().plusDays(7)
    );
  }

  private OAuthUserInfo createOAuthUserInfo() {
    return OAuthUserInfo.builder()
        .providerType(ProviderType.KAKAO)
        .providerUserId("kakao123")
        .email("test@test.com")
        .name("테스트")
        .build();
  }

  @Nested
  class OAuth_인증_URL_생성_테스트 {

    @Test
    void 인증_URL_생성을_성공한다() {
      given(oAuthPort.isProviderConfigured(ProviderType.KAKAO)).willReturn(true);
      given(oAuthPort.getAuthorizationUrl(any(), anyString()))
          .willReturn("https://kauth.kakao.com/oauth/authorize?...");

      String url = oAuthCommandService.getAuthorizationUrl(
          ProviderType.KAKAO, false, "device-info");

      assertThat(url).startsWith("https://kauth.kakao.com");
    }

    @Test
    void 설정되지_않은_제공자는_실패한다() {
      given(oAuthPort.isProviderConfigured(ProviderType.KAKAO)).willReturn(false);

      assertThatThrownBy(() -> oAuthCommandService.getAuthorizationUrl(
          ProviderType.KAKAO, false, "device-info"))
          .isInstanceOf(AuthException.class)
          .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
    }
  }

  @Nested
  class OAuth_롤백_처리_테스트 {

    @Test
    void 신규회원_회원가입_후_로그인이_가능하다() {
      OAuthState state = OAuthState.forLogin(false, "device-info");
      OAuthUserInfo userInfo = createOAuthUserInfo();

      given(oAuthPort.getUserInfo(ProviderType.KAKAO, "code"))
          .willReturn(userInfo);
      given(authRepository.findByProviderAndProviderUserId(any(), anyString()))
          .willReturn(Optional.empty());
      given(authRepository.findByEmailAndUserType(anyString(), any()))
          .willReturn(Optional.empty());
      given(authRepository.save(any(Auth.class)))
          .willAnswer(inv -> inv.getArgument(0));
      given(tokenPort.issueTokens(any(), any(), anyString(), anyBoolean()))
          .willReturn(createTokenResult());

      LoginResult result = oAuthCommandService.handleCallback(
          ProviderType.KAKAO, "code", state.encode());

      assertThat(result).isNotNull();
      assertThat(result.email()).isEqualTo("test@test.com");
      assertThat(result.userType()).isEqualTo(UserType.CUSTOMER);
      verify(authRepository).save(any(Auth.class));
    }

    @Test
    void 기존_회원_제공자_정보로_로그인이_가능하다() {
      Auth existingAuth = Auth.registerWithOAuth(
          "test@test.com", "Password123!", UserType.CUSTOMER,
          ProviderType.KAKAO, "kakao123", passwordEncoder, "SYSTEM");

      OAuthState state = OAuthState.forLogin(false, "device-info");
      OAuthUserInfo userInfo = createOAuthUserInfo();

      given(oAuthPort.getUserInfo(ProviderType.KAKAO, "code"))
          .willReturn(userInfo);
      given(authRepository.findByProviderAndProviderUserId(ProviderType.KAKAO, "kakao123"))
          .willReturn(Optional.of(existingAuth));
      given(tokenPort.issueTokens(any(), any(), anyString(), anyBoolean()))
          .willReturn(createTokenResult());

      LoginResult result = oAuthCommandService.handleCallback(
          ProviderType.KAKAO, "code", state.encode());

      assertThat(result).isNotNull();
      assertThat(result.email()).isEqualTo("test@test.com");
    }

    @Test
    void 기존_회원_이메일로_연동_후_로그인이_가능하다() {
      Auth existingAuth = Auth.register(
          "test@test.com", "Password123!", UserType.CUSTOMER, passwordEncoder, "SYSTEM");

      OAuthState state = OAuthState.forLogin(false, "device-info");
      OAuthUserInfo userInfo = createOAuthUserInfo();

      given(oAuthPort.getUserInfo(ProviderType.KAKAO, "code"))
          .willReturn(userInfo);
      given(authRepository.findByProviderAndProviderUserId(any(), anyString()))
          .willReturn(Optional.empty());
      given(authRepository.findByEmailAndUserType("test@test.com", UserType.CUSTOMER))
          .willReturn(Optional.of(existingAuth));
      given(tokenPort.issueTokens(any(), any(), anyString(), anyBoolean()))
          .willReturn(createTokenResult());

      LoginResult result = oAuthCommandService.handleCallback(
          ProviderType.KAKAO, "code", state.encode());

      assertThat(result).isNotNull();
      assertThat(existingAuth.hasProvider(ProviderType.KAKAO)).isTrue();
    }

    @Test
    void 이메일이_없으면_실패한다() {
      OAuthState state = OAuthState.forLogin(false, "device-info");
      OAuthUserInfo userInfoNoEmail = OAuthUserInfo.builder()
          .providerType(ProviderType.KAKAO)
          .providerUserId("kakao123")
          .email(null)
          .build();

      given(oAuthPort.getUserInfo(ProviderType.KAKAO, "code"))
          .willReturn(userInfoNoEmail);

      assertThatThrownBy(() -> oAuthCommandService.handleCallback(
          ProviderType.KAKAO, "code", state.encode()))
          .isInstanceOf(AuthException.class)
          .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.OAUTH_EMAIL_REQUIRED);
    }

    @Test
    void 잘못된_state는_실패한다() {
      assertThatThrownBy(() -> oAuthCommandService.handleCallback(
          ProviderType.KAKAO, "code", "invalid-state"))
          .isInstanceOf(AuthException.class)
          .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_OAUTH_STATE);
    }
  }

  @Nested
  class 계정_연동_URL_생성_테스트 {

    @Test
    void 연동_URL_생성_성공한다() {
      Auth auth = Auth.register(
          "test@test.com", "Password123!", UserType.CUSTOMER, passwordEncoder, "SYSTEM");
      UUID authId = auth.getId();

      given(authRepository.findById(authId)).willReturn(Optional.of(auth));
      given(oAuthPort.isProviderConfigured(ProviderType.KAKAO)).willReturn(true);
      given(oAuthPort.getAuthorizationUrl(any(), anyString()))
          .willReturn("https://kauth.kakao.com/oauth/authorize?...");

      String url = oAuthCommandService.getLinkUrl(ProviderType.KAKAO, authId, "device-info");

      assertThat(url).startsWith("https://kauth.kakao.com");
    }

    @Test
    void 이미_연동된_제공자는_실패한다() {
      Auth auth = Auth.registerWithOAuth(
          "test@test.com", "Password123!", UserType.CUSTOMER,
          ProviderType.KAKAO, "kakao123", passwordEncoder, "SYSTEM");
      UUID authId = auth.getId();

      given(authRepository.findById(authId)).willReturn(Optional.of(auth));

      assertThatThrownBy(() -> oAuthCommandService.getLinkUrl(
          ProviderType.KAKAO, authId, "device-info"))
          .isInstanceOf(AuthException.class)
          .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.PROVIDER_ALREADY_CONNECTED);
    }

    @Test
    void SELLER는_연동이_불가능하다() {
      Auth auth = Auth.register(
          "test@test.com", "Password123!", UserType.SELLER, passwordEncoder, "SYSTEM");
      UUID authId = auth.getId();

      given(authRepository.findById(authId)).willReturn(Optional.of(auth));

      assertThatThrownBy(() -> oAuthCommandService.getLinkUrl(
          ProviderType.KAKAO, authId, "device-info"))
          .isInstanceOf(AuthException.class)
          .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.OAUTH_NOT_ALLOWED_FOR_USER_TYPE);
    }
  }

  @Nested
  class 연동_해제_테스트 {

    @Test
    void 연동_해제_성공한다() {
      Auth auth = Auth.registerWithOAuth(
          "test@test.com", "Password123!", UserType.CUSTOMER,
          ProviderType.KAKAO, "kakao123", passwordEncoder, "SYSTEM");
      UUID authId = auth.getId();

      given(authRepository.findById(authId)).willReturn(Optional.of(auth));

      oAuthCommandService.unlinkProvider(authId, ProviderType.KAKAO);

      assertThat(auth.hasProvider(ProviderType.KAKAO)).isFalse();
    }

  }
}