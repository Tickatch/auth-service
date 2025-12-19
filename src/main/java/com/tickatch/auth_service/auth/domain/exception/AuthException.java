package com.tickatch.auth_service.auth.domain.exception;

import io.github.tickatch.common.error.BusinessException;

/**
 * Auth 도메인 예외.
 *
 * @author Tickatch
 * @since 1.0.0
 */
public class AuthException extends BusinessException {

  public AuthException(AuthErrorCode errorCode) {
    super(errorCode);
  }

  public AuthException(AuthErrorCode errorCode, Object... errorArgs) {
    super(errorCode, errorArgs);
  }

  public AuthException(AuthErrorCode errorCode, Throwable cause, Object... errorArgs) {
    super(errorCode, cause, errorArgs);
  }
}
