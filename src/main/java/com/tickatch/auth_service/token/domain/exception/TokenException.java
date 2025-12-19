package com.tickatch.auth_service.token.domain.exception;

import io.github.tickatch.common.error.BusinessException;

/**
 * Token 도메인 예외.
 *
 * @author Tickatch
 * @since 1.0.0
 */
public class TokenException extends BusinessException {

  public TokenException(TokenErrorCode errorCode) {
    super(errorCode);
  }

  public TokenException(TokenErrorCode errorCode, Object... errorArgs) {
    super(errorCode, errorArgs);
  }

  public TokenException(TokenErrorCode errorCode, Throwable cause, Object... errorArgs) {
    super(errorCode, cause, errorArgs);
  }
}
