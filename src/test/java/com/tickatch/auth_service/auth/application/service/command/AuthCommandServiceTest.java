package com.tickatch.auth_service.auth.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.tickatch.auth_service.auth.application.port.out.TokenPort;
import com.tickatch.auth_service.auth.application.service.command.dto.ChangePasswordCommand;
import com.tickatch.auth_service.auth.application.service.command.dto.LoginCommand;
import com.tickatch.auth_service.auth.application.service.command.dto.LoginResult;
import com.tickatch.auth_service.auth.application.service.command.dto.LogoutCommand;
import com.tickatch.auth_service.auth.application.service.command.dto.RegisterCommand;
import com.tickatch.auth_service.auth.application.service.command.dto.WithdrawCommand;
import com.tickatch.auth_service.auth.domain.Auth;
import com.tickatch.auth_service.auth.domain.AuthRepository;
import com.tickatch.auth_service.auth.domain.exception.AuthErrorCode;
import com.tickatch.auth_service.auth.domain.exception.AuthException;
import com.tickatch.auth_service.auth.domain.vo.UserType;
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

@DisplayName("AuthCommandService 테스트")
@ExtendWith(MockitoExtension.class)
class AuthCommandServiceTest {

  @InjectMocks
  private AuthCommandService authCommandService;

  @Mock
  private AuthRepository authRepository;

  @Mock
  private TokenPort tokenPort;

  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    passwordEncoder = new BCryptPasswordEncoder();
    authCommandService = new AuthCommandService(authRepository, tokenPort, passwordEncoder);
  }

  private TokenResult createTokenResult() {
    return TokenResult.of(
        "access-token",
        "refresh-token",
        LocalDateTime.now().plusMinutes(5),
        LocalDateTime.now().plusDays(7)
    );
  }

  @Nested
  class 회원가입_테스트 {

    @Test
    void 회원가입을_성공한다() {
      RegisterCommand command = RegisterCommand.of(
          "test@test.com", "Password123!", UserType.CUSTOMER, "device", false);

      given(authRepository.existsByEmailAndUserType(anyString(), any())).willReturn(false);
      given(authRepository.save(any(Auth.class))).willAnswer(inv -> inv.getArgument(0));
      given(tokenPort.issueTokens(any(), any(), anyString(), anyBoolean()))
          .willReturn(createTokenResult());

      LoginResult result = authCommandService.register(command);

      assertThat(result).isNotNull();
      assertThat(result.email()).isEqualTo("test@test.com");
      assertThat(result.userType()).isEqualTo(UserType.CUSTOMER);
      assertThat(result.accessToken()).isEqualTo("access-token");
      verify(authRepository).save(any(Auth.class));
    }

    @Test
    void 이메일이_중복되면_실패한다() {
      RegisterCommand command = RegisterCommand.of(
          "test@test.com", "Password123!", UserType.CUSTOMER, "device", false);

      given(authRepository.existsByEmailAndUserType(anyString(), any())).willReturn(true);

      assertThatThrownBy(() -> authCommandService.register(command))
          .isInstanceOf(AuthException.class)
          .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.EMAIL_ALREADY_EXISTS);
    }
  }

  @Nested
  class 로그인_테스트 {

    @Test
    void 로그인을_성공한다() {
      String rawPassword = "Password123!";
      Auth auth = Auth.register("test@test.com", rawPassword, UserType.CUSTOMER,
          passwordEncoder, "SYSTEM");

      LoginCommand command = LoginCommand.of(
          "test@test.com", rawPassword, UserType.CUSTOMER, "device", false);

      given(authRepository.findByEmailAndUserType(anyString(), any()))
          .willReturn(Optional.of(auth));
      given(tokenPort.issueTokens(any(), any(), anyString(), anyBoolean()))
          .willReturn(createTokenResult());

      LoginResult result = authCommandService.login(command);

      assertThat(result).isNotNull();
      assertThat(result.email()).isEqualTo("test@test.com");
      assertThat(result.accessToken()).isEqualTo("access-token");
    }

    @Test
    void 비밀번호_불일치_시_실패한다() {
      Auth auth = Auth.register("test@test.com", "Password123!", UserType.CUSTOMER,
          passwordEncoder, "SYSTEM");

      LoginCommand command = LoginCommand.of(
          "test@test.com", "wrongPassword", UserType.CUSTOMER, "device", false);

      given(authRepository.findByEmailAndUserType(anyString(), any()))
          .willReturn(Optional.of(auth));

      assertThatThrownBy(() -> authCommandService.login(command))
          .isInstanceOf(AuthException.class)
          .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    void 존재하지_않는_계정으로_로그인_시_실패한다() {
      LoginCommand command = LoginCommand.of(
          "notfound@test.com", "Password123!", UserType.CUSTOMER, "device", false);

      given(authRepository.findByEmailAndUserType(anyString(), any()))
          .willReturn(Optional.empty());

      assertThatThrownBy(() -> authCommandService.login(command))
          .isInstanceOf(AuthException.class)
          .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_CREDENTIALS);
    }
  }

  @Nested
  class 로그아웃_테스트 {

    @Test
    void 단일_기기로_로그아웃_한다() {
      UUID authId = UUID.randomUUID();
      LogoutCommand command = LogoutCommand.of(authId, "refresh-token", false);

      authCommandService.logout(command);

      verify(tokenPort).revokeToken("refresh-token");
    }

    @Test
    void 전체_기기로_로그아웃_한다() {
      UUID authId = UUID.randomUUID();
      LogoutCommand command = LogoutCommand.of(authId, "refresh-token", true);

      authCommandService.logout(command);

      verify(tokenPort).revokeAllTokens(authId);
    }
  }

  @Nested
  class 비밀번호_변경_테스트 {

    @Test
    void 비밀번호_변경_성공한다() {
      String currentPassword = "Password123!";
      Auth auth = Auth.register("test@test.com", currentPassword, UserType.CUSTOMER,
          passwordEncoder, "SYSTEM");
      UUID authId = auth.getId();

      ChangePasswordCommand command = ChangePasswordCommand.of(
          authId, currentPassword, "NewPassword456!");

      given(authRepository.findById(authId)).willReturn(Optional.of(auth));

      authCommandService.changePassword(command);

      verify(tokenPort).revokeAllTokens(authId);
      assertThat(auth.matchesPassword("NewPassword456!", passwordEncoder)).isTrue();
    }

    @Test
    void 현재_비밀번호_불일치_시_실패한다() {
      Auth auth = Auth.register("test@test.com", "Password123!", UserType.CUSTOMER,
          passwordEncoder, "SYSTEM");
      UUID authId = auth.getId();

      ChangePasswordCommand command = ChangePasswordCommand.of(
          authId, "wrongPassword", "NewPassword456!");

      given(authRepository.findById(authId)).willReturn(Optional.of(auth));

      assertThatThrownBy(() -> authCommandService.changePassword(command))
          .isInstanceOf(AuthException.class)
          .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_CURRENT_PASSWORD);
    }
  }

  @Nested
  class 회원_탈퇴_테스트 {

    @Test
    void 회원탈퇴를_성공한다() {
      String password = "Password123!";
      Auth auth = Auth.register("test@test.com", password, UserType.CUSTOMER,
          passwordEncoder, "SYSTEM");
      UUID authId = auth.getId();

      WithdrawCommand command = WithdrawCommand.of(authId, password);

      given(authRepository.findById(authId)).willReturn(Optional.of(auth));

      authCommandService.withdraw(command);

      verify(tokenPort).deleteAllTokens(authId);
      assertThat(auth.getStatus().isWithdrawn()).isTrue();
    }

    @Test
    void 비밀번호_불일치_시_실패() {
      Auth auth = Auth.register("test@test.com", "Password123!", UserType.CUSTOMER,
          passwordEncoder, "SYSTEM");
      UUID authId = auth.getId();

      WithdrawCommand command = WithdrawCommand.of(authId, "wrongPassword");

      given(authRepository.findById(authId)).willReturn(Optional.of(auth));

      assertThatThrownBy(() -> authCommandService.withdraw(command))
          .isInstanceOf(AuthException.class)
          .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.INVALID_CREDENTIALS);
    }
  }
}