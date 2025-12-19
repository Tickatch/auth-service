package com.tickatch.auth_service.auth.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("AuthErrorCode 테스트")
class AuthErrorCodeTest {

  @Nested
  class HTTP_상태_코드_테스트 {

    @Test
    void 에러코드_404는_NOT_FOUND_상태를_가진다() {

      assertThat(AuthErrorCode.AUTH_NOT_FOUND.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
      assertThat(AuthErrorCode.PROVIDER_NOT_FOUND.getStatus())
          .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void 에러코드_400은_BAD_REQUEST_상태를_가진다() {

      assertThat(AuthErrorCode.INVALID_EMAIL.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
      assertThat(AuthErrorCode.INVALID_PASSWORD.getStatus())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
      assertThat(AuthErrorCode.INVALID_USER_TYPE.getStatus())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
      assertThat(AuthErrorCode.DUPLICATE_EMAIL.getStatus())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
      assertThat(AuthErrorCode.PASSWORD_MISMATCH.getStatus())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
      assertThat(AuthErrorCode.SAME_AS_OLD_PASSWORD.getStatus())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
      assertThat(AuthErrorCode.PASSWORD_TOO_SHORT.getStatus())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
      assertThat(AuthErrorCode.PASSWORD_TOO_WEAK.getStatus())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
      assertThat(AuthErrorCode.INVALID_PROVIDER.getStatus())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
      assertThat(AuthErrorCode.INVALID_OAUTH_CODE.getStatus())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
      assertThat(AuthErrorCode.PROVIDER_ALREADY_CONNECTED.getStatus())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
      assertThat(AuthErrorCode.OAUTH_NOT_ALLOWED_FOR_USER_TYPE.getStatus())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void 에러코드_401은_UNAUTHORIZED_상태를_가진다() {

      assertThat(AuthErrorCode.INVALID_CREDENTIALS.getStatus())
          .isEqualTo(HttpStatus.UNAUTHORIZED.value());
      assertThat(AuthErrorCode.AUTHENTICATION_FAILED.getStatus())
          .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void 에러코드_403은_FORBIDDEN_상태를_가진다() {

      assertThat(AuthErrorCode.ACCOUNT_LOCKED.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
      assertThat(AuthErrorCode.ACCOUNT_WITHDRAWN.getStatus())
          .isEqualTo(HttpStatus.FORBIDDEN.value());
      assertThat(AuthErrorCode.ACCESS_DENIED.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    }

    @Test
    void 에러코드_422는_UNPROCESSABLE_ENTITY_상태를_가진다() {

      assertThat(AuthErrorCode.LOGIN_FAILED_LIMIT_EXCEEDED.getStatus())
          .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
      assertThat(AuthErrorCode.ALREADY_WITHDRAWN.getStatus())
          .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    }

    @Test
    void 에러코드_503은_SERVICE_UNAVAILABLE_상태를_가진다() {

      assertThat(AuthErrorCode.OAUTH_SERVER_ERROR.getStatus())
          .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
      assertThat(AuthErrorCode.EVENT_PUBLISH_FAILED.getStatus())
          .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    }
  }

  @Nested
  class 에러코드_테스트 {

    @Test
    void 각_에러코드는_고유한_코드_문자열을_가진다() {

      assertThat(AuthErrorCode.AUTH_NOT_FOUND.getCode()).isEqualTo("AUTH_NOT_FOUND");
      assertThat(AuthErrorCode.INVALID_EMAIL.getCode()).isEqualTo("INVALID_EMAIL");
      assertThat(AuthErrorCode.ACCOUNT_LOCKED.getCode()).isEqualTo("ACCOUNT_LOCKED");
    }
  }

  @Nested
  class Vales_테스트 {

    @Test
    void 에러코드는_총_21개_존재한다() {

      assertThat(AuthErrorCode.values()).hasSize(24);
    }
  }
}
