package com.tickatch.auth_service.auth.application.service.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import com.tickatch.auth_service.auth.application.service.query.dto.AuthInfo;
import com.tickatch.auth_service.auth.domain.Auth;
import com.tickatch.auth_service.auth.domain.AuthRepository;
import com.tickatch.auth_service.auth.domain.exception.AuthErrorCode;
import com.tickatch.auth_service.auth.domain.exception.AuthException;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@DisplayName("AuthQueryService 테스트")
@ExtendWith(MockitoExtension.class)
class AuthQueryServiceTest {

  @InjectMocks private AuthQueryService authQueryService;

  @Mock private AuthRepository authRepository;

  private Auth createAuth() {
    return Auth.register(
        "test@test.com", "Password123!", UserType.CUSTOMER, new BCryptPasswordEncoder(), "SYSTEM");
  }

  @Nested
  class findById_테스트 {

    @Test
    void Auth_ID로_조회_성공한다() {
      Auth auth = createAuth();
      UUID authId = auth.getId();

      given(authRepository.findById(authId)).willReturn(Optional.of(auth));

      AuthInfo result = authQueryService.findById(authId);

      assertThat(result).isNotNull();
      assertThat(result.email()).isEqualTo("test@test.com");
      assertThat(result.userType()).isEqualTo(UserType.CUSTOMER);
    }

    @Test
    void 존재하지_않는_ID_조회_시_실패한다() {
      UUID authId = UUID.randomUUID();
      given(authRepository.findById(authId)).willReturn(Optional.empty());

      assertThatThrownBy(() -> authQueryService.findById(authId))
          .isInstanceOf(AuthException.class)
          .hasFieldOrPropertyWithValue("errorCode", AuthErrorCode.AUTH_NOT_FOUND);
    }
  }

  @Nested
  class findByIdOptional_테스트 {

    @Test
    void 존재하면_Optional_of_반환한다() {
      Auth auth = createAuth();
      UUID authId = auth.getId();

      given(authRepository.findById(authId)).willReturn(Optional.of(auth));

      Optional<AuthInfo> result = authQueryService.findByIdOptional(authId);

      assertThat(result).isPresent();
      assertThat(result.get().email()).isEqualTo("test@test.com");
    }

    @Test
    void 존재하지_않으면_Optional_empty_반환한다() {
      UUID authId = UUID.randomUUID();
      given(authRepository.findById(authId)).willReturn(Optional.empty());

      Optional<AuthInfo> result = authQueryService.findByIdOptional(authId);

      assertThat(result).isEmpty();
    }
  }

  @Nested
  class findByEmailAndUserType_테스트 {

    @Test
    void 이메일과_사용자_유형으로_조회_성공한다() {
      Auth auth = createAuth();

      given(authRepository.findByEmailAndUserType("test@test.com", UserType.CUSTOMER))
          .willReturn(Optional.of(auth));

      Optional<AuthInfo> result =
          authQueryService.findByEmailAndUserType("test@test.com", UserType.CUSTOMER);

      assertThat(result).isPresent();
      assertThat(result.get().email()).isEqualTo("test@test.com");
    }

    @Test
    void 존재하지_않으면_Optional_empty로_반환한다() {
      given(authRepository.findByEmailAndUserType(anyString(), any())).willReturn(Optional.empty());

      Optional<AuthInfo> result =
          authQueryService.findByEmailAndUserType("notfound@test.com", UserType.CUSTOMER);

      assertThat(result).isEmpty();
    }
  }

  @Nested
  class findByProviderInfo_테스트 {

    @Test
    void 소셜_로그인_정보로_조회를_성공한다() {
      Auth auth =
          Auth.registerWithOAuth(
              "test@test.com",
              "Password123!",
              UserType.CUSTOMER,
              ProviderType.KAKAO,
              "kakao123",
              new BCryptPasswordEncoder(),
              "SYSTEM");

      given(authRepository.findByProviderAndProviderUserId(ProviderType.KAKAO, "kakao123"))
          .willReturn(Optional.of(auth));

      Optional<AuthInfo> result =
          authQueryService.findByProviderInfo(ProviderType.KAKAO, "kakao123");

      assertThat(result).isPresent();
      assertThat(result.get().email()).isEqualTo("test@test.com");
    }
  }

  @Nested
  class existsByEmailAndUserType_테스트 {

    @Test
    void 이메일이_중복이면_true를_반환한다() {
      given(authRepository.existsByEmailAndUserType("test@test.com", UserType.CUSTOMER))
          .willReturn(true);

      boolean result =
          authQueryService.existsByEmailAndUserType("test@test.com", UserType.CUSTOMER);

      assertThat(result).isTrue();
    }

    @Test
    void 이메일이_중복이_아니면_false_를_반환한다() {
      given(authRepository.existsByEmailAndUserType("new@test.com", UserType.CUSTOMER))
          .willReturn(false);

      boolean result = authQueryService.existsByEmailAndUserType("new@test.com", UserType.CUSTOMER);

      assertThat(result).isFalse();
    }
  }
}
