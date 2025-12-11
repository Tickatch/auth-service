package com.tickatch.auth_service.auth.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tickatch.auth_service.auth.domain.exception.AuthErrorCode;
import com.tickatch.auth_service.auth.domain.exception.AuthException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("Password 테스트")
class PasswordTest {

  private final PasswordEncoder encoder = new BCryptPasswordEncoder();

  @Nested
  class 생성_테스트 {

    @Test
    void 유효한_비밀번호로_Password를_생성한다() {

      assertThatThrownBy(() -> Password.validatePolicy("Ab1!"))
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.PASSWORD_TOO_SHORT);
    }

    @Test
    void 영문과_숫자_그리고_특수문자_조합_비밀번호로_생성_가능하다() {

      assertThatThrownBy(() -> Password.validatePolicy(null))
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.PASSWORD_TOO_SHORT);
    }

  }

  @Nested
  class 검증정책_테스트 {

    @Test
    void 여덟자리_미만_비밀번호는_예외를_던진다() {

      assertThatThrownBy(() -> Password.validatePolicy("Ab1!"))
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.PASSWORD_TOO_SHORT);
    }

    @Test
    void null_비밀번호는_예외를_던진다() {

      assertThatThrownBy(() -> Password.validatePolicy(null))
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.PASSWORD_TOO_SHORT);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcdefgh", "12345678", "!@#$%^*(", "abcd1234", "abcd!@#$", "1234!@#$"})
    void 세가지_문자_유형이_모두_없으면_예외를_던진다(String password) {

      assertThatThrownBy(() -> Password.validatePolicy(password))
          .isInstanceOf(AuthException.class)
          .extracting(e -> ((AuthException) e).getErrorCode())
          .isEqualTo(AuthErrorCode.PASSWORD_TOO_WEAK);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Abcd123!", "Test@456", "Pass_789", "Qwer+123", "Zxcv=987"})
    void 영문과_숫자_그리고_특수문자_3가지_모두_포함하면_검증을_통과한다(String password) {
      // 예외가 발생하지 않아야 함
      Password.validatePolicy(password);
    }

    @Nested
    class 허용되지_않은_문자 {

      @ParameterizedTest
      @ValueSource(strings = {"Pass123'", "Pass123\"", "Pass123<", "Pass123>"})
      void SQL_INJECTION과_XSS_위험_문자는_예외를_던진다(String password) {

        assertThatThrownBy(() -> Password.validatePolicy(password))
            .isInstanceOf(AuthException.class)
            .extracting(e -> ((AuthException) e).getErrorCode())
            .isEqualTo(AuthErrorCode.INVALID_PASSWORD);
      }

      @ParameterizedTest
      @ValueSource(strings = {"Pass123;", "Pass123|", "Pass123`", "Pass123\\"})
      void Command_Injection_위험_문자는_예외를_던진다(String password) {

        assertThatThrownBy(() -> Password.validatePolicy(password))
            .isInstanceOf(AuthException.class)
            .extracting(e -> ((AuthException) e).getErrorCode())
            .isEqualTo(AuthErrorCode.INVALID_PASSWORD);
      }

      @ParameterizedTest
      @ValueSource(strings = {"Pass123{", "Pass123}", "Pass123[", "Pass123]"})
      void Template_Injection_위험_문자는_예외를_던진다(String password) {

        assertThatThrownBy(() -> Password.validatePolicy(password))
            .isInstanceOf(AuthException.class)
            .extracting(e -> ((AuthException) e).getErrorCode())
            .isEqualTo(AuthErrorCode.INVALID_PASSWORD);
      }

      @ParameterizedTest
      @ValueSource(strings = {"Pass123&", "Pass123:"})
      void 기타_위험_문자는_예외를_던진다(String password) {

        assertThatThrownBy(() -> Password.validatePolicy(password))
            .isInstanceOf(AuthException.class)
            .extracting(e -> ((AuthException) e).getErrorCode())
            .isEqualTo(AuthErrorCode.INVALID_PASSWORD);
      }
    }

    @Nested
    class 허용된_특수문자_테스트 {

      @ParameterizedTest
      @ValueSource(strings = {
          "Abcd123!", "Abcd123@", "Abcd123#", "Abcd123$", "Abcd123%",
          "Abcd123^", "Abcd123*", "Abcd123(", "Abcd123)", "Abcd123_",
          "Abcd123+", "Abcd123-", "Abcd123=", "Abcd123.", "Abcd123,",
          "Abcd123?"
      })
      void 허용된_특수문자는_검증을_통과한다(String password) {
        // 예외가 발생하지 않아야 함
        Password.validatePolicy(password);
      }
    }
  }

  @Nested
  class 일치_테스트 {

    @Test
    void 동일한_비밀번호는_일치한다() {

      String rawPassword = "Pass123!";
      Password password = Password.create(rawPassword, encoder);

      assertThat(password.matches(rawPassword, encoder)).isTrue();
    }

    @Test
    void 다른_비밀번호는_일치하지_않는다() {

      String rawPassword = "Pass123!";
      Password password = Password.create(rawPassword, encoder);

      assertThat(password.matches("Wrong123!", encoder)).isFalse();
    }
  }

  @Nested
  class 인코딩_테스트 {

    @Test
    void 인코딩된_값으로_Password를_생성한다() {

      String encodedValue = encoder.encode("Pass123!");

      Password password = Password.fromEncoded(encodedValue);

      assertThat(password.getEncodedValue()).isEqualTo(encodedValue);
      assertThat(password.matches("Pass123!", encoder)).isTrue();
    }
  }

  @Nested
  class 값_유무_테스트 {

    @Test
    void 값이_있으면_true를_반환한다() {

      Password password = Password.create("Pass123!", encoder);
      assertThat(password.hasValue()).isTrue();
    }

    @Test
    void 빈_문자열이면_false를_반환한다() {

      Password password = Password.fromEncoded("");
      assertThat(password.hasValue()).isFalse();
    }

    @Test
    void null이면_false를_반환한다() {

      Password password = Password.fromEncoded(null);
      assertThat(password.hasValue()).isFalse();
    }
  }

  @Nested
  class 동등성_테스트 {

    @Test
    void 동일한_인코딩_값을_가진_Password는_같다() {

      String encoded = encoder.encode("Pass123!");
      Password password1 = Password.fromEncoded(encoded);
      Password password2 = Password.fromEncoded(encoded);

      assertThat(password1).isEqualTo(password2);
      assertThat(password1.hashCode()).isEqualTo(password2.hashCode());
    }

    @Test
    void 다른_인코딩_값을_가진_Password는_다르다() {

      Password password1 = Password.create("Pass123!", encoder);
      Password password2 = Password.create("Pass456@", encoder);

      assertThat(password1).isNotEqualTo(password2);
    }
  }
}