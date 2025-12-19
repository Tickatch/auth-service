package com.tickatch.auth_service.token.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

@DisplayName("TokenErrorCode 테스트")
class TokenErrorCodeTest {

  @Nested
  class HTTP_상태코드_테스트 {

    @Test
    void 에러코드_404는_NOT_FOUND_상태를_가진다() {

      assertThat(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND.getStatus())
          .isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void 에러코드_400은_BAD_REQUEST_상태를_가진다() {

      assertThat(TokenErrorCode.INVALID_TOKEN.getStatus())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
      assertThat(TokenErrorCode.INVALID_REFRESH_TOKEN.getStatus())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
      assertThat(TokenErrorCode.INVALID_TOKEN_FORMAT.getStatus())
          .isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void 에러코드_401은_UNAUTHORIZED_상태를_가진다() {

      assertThat(TokenErrorCode.TOKEN_EXPIRED.getStatus())
          .isEqualTo(HttpStatus.UNAUTHORIZED.value());
      assertThat(TokenErrorCode.REFRESH_TOKEN_EXPIRED.getStatus())
          .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    void 에러코드_422는_UNPROCESSABLE_ENTITY_상태를_가진다() {

      assertThat(TokenErrorCode.TOKEN_ALREADY_REVOKED.getStatus())
          .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
      assertThat(TokenErrorCode.TOKEN_REUSE_DETECTED.getStatus())
          .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
    }
  }

  @Nested
  class 에러코드_테스트 {

    @Test
    void 각_에러코드는_고유한_코드_문자열을_가진다() {

      assertThat(TokenErrorCode.REFRESH_TOKEN_NOT_FOUND.getCode())
          .isEqualTo("REFRESH_TOKEN_NOT_FOUND");
      assertThat(TokenErrorCode.INVALID_TOKEN.getCode()).isEqualTo("INVALID_TOKEN");
      assertThat(TokenErrorCode.TOKEN_EXPIRED.getCode()).isEqualTo("TOKEN_EXPIRED");
      assertThat(TokenErrorCode.TOKEN_ALREADY_REVOKED.getCode()).isEqualTo("TOKEN_ALREADY_REVOKED");
    }
  }

  @Nested
  class Values_테스트 {

    @Test
    void 에러코드가_8개_존재한다() {

      assertThat(TokenErrorCode.values()).hasSize(8);
    }
  }
}
