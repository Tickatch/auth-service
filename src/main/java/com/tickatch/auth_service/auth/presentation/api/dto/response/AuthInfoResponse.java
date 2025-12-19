package com.tickatch.auth_service.auth.presentation.api.dto.response;

import com.tickatch.auth_service.auth.application.service.query.dto.AuthInfo;
import com.tickatch.auth_service.auth.domain.vo.AuthStatus;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 내 정보 조회 응답 DTO.
 *
 * @param id Auth ID
 * @param email 이메일
 * @param userType 사용자 유형
 * @param status 계정 상태
 * @param lastLoginAt 마지막 로그인 일시
 * @param providers 연동된 소셜 로그인 제공자 목록
 * @param createdAt 생성 일시
 */
public record AuthInfoResponse(
    UUID id,
    String email,
    UserType userType,
    AuthStatus status,
    LocalDateTime lastLoginAt,
    List<ProviderType> providers,
    LocalDateTime createdAt) {

  public AuthInfoResponse {
    providers = providers == null ? List.of() : List.copyOf(providers);
  }

  /**
   * AuthInfo에서 변환한다.
   *
   * @param info Auth 정보
   * @return AuthInfoResponse
   */
  public static AuthInfoResponse from(AuthInfo info) {
    return new AuthInfoResponse(
        info.id(),
        info.email(),
        info.userType(),
        info.status(),
        info.lastLoginAt(),
        info.providers(),
        info.createdAt());
  }
}
