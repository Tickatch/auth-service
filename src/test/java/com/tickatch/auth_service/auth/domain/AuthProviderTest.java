package com.tickatch.auth_service.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("AuthProvider 테스트")
class AuthProviderTest {

  private final PasswordEncoder encoder = new BCryptPasswordEncoder();

  @Nested
  class 생성_테스트 {

    @Test
    void AuthProvider를_생성한다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");

      AuthProvider provider = AuthProvider.create(auth, ProviderType.KAKAO, "kakao123");

      assertThat(provider.getId()).isNotNull();
      assertThat(provider.getAuth()).isEqualTo(auth);
      assertThat(provider.getProvider()).isEqualTo(ProviderType.KAKAO);
      assertThat(provider.getProviderUserId()).isEqualTo("kakao123");
      assertThat(provider.getConnectedAt()).isNotNull();
    }

    @Test
    void 각_제공자_타입으로_생성할_수_있다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");

      AuthProvider kakaoProvider = AuthProvider.create(auth, ProviderType.KAKAO, "kakao123");
      AuthProvider naverProvider = AuthProvider.create(auth, ProviderType.NAVER, "naver123");
      AuthProvider googleProvider = AuthProvider.create(auth, ProviderType.GOOGLE, "google123");

      assertThat(kakaoProvider.getProvider()).isEqualTo(ProviderType.KAKAO);
      assertThat(naverProvider.getProvider()).isEqualTo(ProviderType.NAVER);
      assertThat(googleProvider.getProvider()).isEqualTo(ProviderType.GOOGLE);
    }
  }

  @Nested
  class 동등성_테스트 {

    @Test
    void 동일한_ID를_가진_AuthProvider는_길다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      AuthProvider provider1 = AuthProvider.create(auth, ProviderType.KAKAO, "kakao123");
      AuthProvider provider2 = provider1;

      assertThat(provider1).isEqualTo(provider2);
      assertThat(provider1.hashCode()).isEqualTo(provider2.hashCode());
    }

    @Test
    void 다른_ID를_가진_AuthProvider는_다르다() {
      Auth auth =
          Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      AuthProvider provider1 = AuthProvider.create(auth, ProviderType.KAKAO, "kakao123");
      AuthProvider provider2 = AuthProvider.create(auth, ProviderType.KAKAO, "kakao123");

      assertThat(provider1).isNotEqualTo(provider2);
    }
  }
}
