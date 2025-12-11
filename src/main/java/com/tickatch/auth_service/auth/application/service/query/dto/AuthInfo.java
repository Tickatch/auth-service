package com.tickatch.auth_service.auth.application.service.query.dto;

import com.tickatch.auth_service.auth.domain.Auth;
import com.tickatch.auth_service.auth.domain.vo.AuthStatus;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Auth 정보 조회 결과.
 *
 * @param id Auth ID
 * @param email 이메일
 * @param userType 사용자 유형
 * @param status 계정 상태
 * @param lastLoginAt 마지막 로그인 일시
 * @param providers 연동된 소셜 로그인 제공자 목록
 * @param createdAt 생성 일시
 */
public record AuthInfo(
    UUID id,
    String email,
    UserType userType,
    AuthStatus status,
    LocalDateTime lastLoginAt,
    List<ProviderType> providers,
    LocalDateTime createdAt
) {

  public static AuthInfo from(Auth auth) {
    List<ProviderType> providers = auth.getProviders().stream()
        .map(p -> p.getProvider())
        .toList();

    return new AuthInfo(
        auth.getId(),
        auth.getEmail(),
        auth.getUserType(),
        auth.getStatus(),
        auth.getLastLoginAt(),
        providers,
        auth.getCreatedAt()
    );
  }
}