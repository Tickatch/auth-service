package com.tickatch.auth_service.token.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TokenException 테스트")
class TokenExceptionTest {

  @Nested
  class 생성자_테스트 {

    @Test
    void 에러코드로_예외를_생성한다() {

      TokenException exception = new TokenException(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND);

      assertThat(exception.getErrorCode()).isEqualTo(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND);
    }

    @Test
    void 에러코드와_인자로_예외를_생성한다() {

      TokenException exception = new TokenException(TokenErrorCode.INVALID_TOKEN, "token-value");

      assertThat(exception.getErrorCode()).isEqualTo(TokenErrorCode.INVALID_TOKEN);
      assertThat(exception.getErrorArgs()).containsExactly("token-value");
    }

    @Test
    void 에러코드와_원인_예외로_예외를_생성한다() {

      RuntimeException cause = new RuntimeException("원인 예외");

      TokenException exception = new TokenException(TokenErrorCode.INVALID_TOKEN_FORMAT, cause);

      assertThat(exception.getErrorCode()).isEqualTo(TokenErrorCode.INVALID_TOKEN_FORMAT);
      assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void 에러코드와_원인_예외_그리고_인자로_예외를_생성한다() {
      RuntimeException cause = new RuntimeException("원인 예외");

      TokenException exception = new TokenException(TokenErrorCode.TOKEN_REUSE_DETECTED, cause, "token-id");

      assertThat(exception.getErrorCode()).isEqualTo(TokenErrorCode.TOKEN_REUSE_DETECTED);
      assertThat(exception.getCause()).isEqualTo(cause);
      assertThat(exception.getErrorArgs()).containsExactly("token-id");
    }
  }

  @Nested
  class getErrorCode_테스트 {

    @Test
    void TokenErrorCode_타입을_반환한다() {

      TokenException exception = new TokenException(TokenErrorCode.TOKEN_EXPIRED);

      assertThat(exception.getErrorCode()).isInstanceOf(TokenErrorCode.class);
    }
  }
}