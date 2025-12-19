package com.tickatch.auth_service.auth.presentation.api.dto.response;

/**
 * 이메일 중복 확인 응답 DTO.
 *
 * @param available 사용 가능 여부 (true: 사용 가능, false: 이미 존재)
 */
public record CheckEmailResponse(boolean available) {

  public static CheckEmailResponse of(boolean exists) {
    return new CheckEmailResponse(!exists);
  }
}
