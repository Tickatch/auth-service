package com.tickatch.auth_service.auth.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AuthException 테스트")
class AuthExceptionTest {

  @Nested
  class 생성자_테스트 {

    @Test
    void 에러코드로_예외를_생성한다() {

      AuthException exception = new AuthException(AuthErrorCode.AUTH_NOT_FOUND);

      assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.AUTH_NOT_FOUND);
    }

    @Test
    void 에러코드와_인자로_예외를_생성한다() {

      AuthException exception = new AuthException(AuthErrorCode.PROVIDER_ALREADY_CONNECTED, "KAKAO");

      assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.PROVIDER_ALREADY_CONNECTED);
      assertThat(exception.getErrorArgs()).containsExactly("KAKAO");
    }

    @Test
    void 에러코드와_원인_예외로_예외를_생성한다() {

      RuntimeException cause = new RuntimeException("원인 예외");

      AuthException exception = new AuthException(AuthErrorCode.OAUTH_SERVER_ERROR, cause);

      assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.OAUTH_SERVER_ERROR);
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void 에러코드와_원인_예외_인자로_예외를_생성한다() {

      RuntimeException cause = new RuntimeException("원인 예외");

      AuthException exception = new AuthException(AuthErrorCode.OAUTH_SERVER_ERROR, cause, "KAKAO");

      assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.OAUTH_SERVER_ERROR);
      assertThat(exception.getCause()).isEqualTo(cause);
      assertThat(exception.getErrorArgs()).containsExactly("KAKAO");
    }
  }

  @Nested
  class GetErrorCode_메서드 {

    @Test
    void AuthErrorCode_타입을_반환한다() {

      AuthException exception = new AuthException(AuthErrorCode.INVALID_EMAIL);

      assertThat(exception.getErrorCode()).isInstanceOf(AuthErrorCode.class);
    }
  }
}