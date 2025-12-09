package com.tickatch.auth_service.auth.domain.exception;

import io.github.tickatch.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Auth 전용 에러코드.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

  // ========================================
  // 조회 (404)
  // ========================================
  AUTH_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "AUTH_NOT_FOUND"),
  PROVIDER_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "PROVIDER_NOT_FOUND"),

  // ========================================
  // 검증 - 회원가입 (400)
  // ========================================
  INVALID_EMAIL(HttpStatus.BAD_REQUEST.value(), "INVALID_EMAIL"),
  INVALID_PASSWORD(HttpStatus.BAD_REQUEST.value(), "INVALID_PASSWORD"),
  INVALID_USER_TYPE(HttpStatus.BAD_REQUEST.value(), "INVALID_USER_TYPE"),
  DUPLICATE_EMAIL(HttpStatus.BAD_REQUEST.value(), "DUPLICATE_EMAIL"),

  // ========================================
  // 검증 - 비밀번호 (400)
  // ========================================
  PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST.value(), "PASSWORD_MISMATCH"),
  SAME_AS_OLD_PASSWORD(HttpStatus.BAD_REQUEST.value(), "SAME_AS_OLD_PASSWORD"),
  PASSWORD_TOO_SHORT(HttpStatus.BAD_REQUEST.value(), "PASSWORD_TOO_SHORT"),
  PASSWORD_TOO_WEAK(HttpStatus.BAD_REQUEST.value(), "PASSWORD_TOO_WEAK"),

  // ========================================
  // 검증 - 소셜 로그인 (400)
  // ========================================
  INVALID_PROVIDER(HttpStatus.BAD_REQUEST.value(), "INVALID_PROVIDER"),
  INVALID_OAUTH_CODE(HttpStatus.BAD_REQUEST.value(), "INVALID_OAUTH_CODE"),
  PROVIDER_ALREADY_CONNECTED(HttpStatus.BAD_REQUEST.value(), "PROVIDER_ALREADY_CONNECTED"),
  OAUTH_NOT_ALLOWED_FOR_USER_TYPE(HttpStatus.BAD_REQUEST.value(), "OAUTH_NOT_ALLOWED_FOR_USER_TYPE"),

  // ========================================
  // 인증 실패 (401)
  // ========================================
  INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED.value(), "INVALID_CREDENTIALS"),
  AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED.value(), "AUTHENTICATION_FAILED"),

  // ========================================
  // 권한 없음 (403)
  // ========================================
  ACCOUNT_LOCKED(HttpStatus.FORBIDDEN.value(), "ACCOUNT_LOCKED"),
  ACCOUNT_WITHDRAWN(HttpStatus.FORBIDDEN.value(), "ACCOUNT_WITHDRAWN"),
  ACCESS_DENIED(HttpStatus.FORBIDDEN.value(), "ACCESS_DENIED"),

  // ========================================
  // 비즈니스 규칙 (422)
  // ========================================
  LOGIN_FAILED_LIMIT_EXCEEDED(
      HttpStatus.UNPROCESSABLE_ENTITY.value(), "LOGIN_FAILED_LIMIT_EXCEEDED"),
  CANNOT_DISCONNECT_LAST_PROVIDER(
      HttpStatus.UNPROCESSABLE_ENTITY.value(), "CANNOT_DISCONNECT_LAST_PROVIDER"),
  ALREADY_WITHDRAWN(HttpStatus.UNPROCESSABLE_ENTITY.value(), "ALREADY_WITHDRAWN"),

  // ========================================
  // 외부 서비스 (503)
  // ========================================
  OAUTH_SERVER_ERROR(HttpStatus.SERVICE_UNAVAILABLE.value(), "OAUTH_SERVER_ERROR"),
  EVENT_PUBLISH_FAILED(HttpStatus.SERVICE_UNAVAILABLE.value(), "EVENT_PUBLISH_FAILED");

  private final int status;
  private final String code;
}