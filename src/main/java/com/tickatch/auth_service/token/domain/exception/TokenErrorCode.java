package com.tickatch.auth_service.token.domain.exception;

import io.github.tickatch.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Token 전용 에러코드.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum TokenErrorCode implements ErrorCode {

  // ========================================
  // 조회 (404)
  // ========================================
  REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "REFRESH_TOKEN_NOT_FOUND"),
  AUTH_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "AUTH_NOT_FOUND"),

  // ========================================
  // 검증 (400)
  // ========================================
  INVALID_TOKEN(HttpStatus.BAD_REQUEST.value(), "INVALID_TOKEN"),
  INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST.value(), "INVALID_REFRESH_TOKEN"),
  INVALID_TOKEN_FORMAT(HttpStatus.BAD_REQUEST.value(), "INVALID_TOKEN_FORMAT"),

  // ========================================
  // 만료 (401)
  // ========================================
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED.value(), "TOKEN_EXPIRED"),
  REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED.value(), "REFRESH_TOKEN_EXPIRED"),

  // ========================================
  // 비즈니스 규칙 (422)
  // ========================================
  TOKEN_ALREADY_REVOKED(HttpStatus.UNPROCESSABLE_ENTITY.value(), "TOKEN_ALREADY_REVOKED"),
  TOKEN_REUSE_DETECTED(HttpStatus.UNPROCESSABLE_ENTITY.value(), "TOKEN_REUSE_DETECTED");

  private final int status;
  private final String code;
}