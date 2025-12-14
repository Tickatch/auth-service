package com.tickatch.auth_service.auth.domain.repository.dto;

import com.tickatch.auth_service.auth.domain.Auth;
import com.tickatch.auth_service.auth.domain.AuthProvider;
import com.tickatch.auth_service.auth.domain.vo.AuthStatus;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * Auth 응답 DTO.
 *
 * <p>Auth 조회 시 반환되는 데이터를 담는다. 엔티티를 외부에 노출하지 않고 필요한 필드만 전달한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Getter
@Builder
public class AuthResponse {

  // ========== 기본 정보 ==========

  /** 인증 ID */
  private final UUID id;

  /** 이메일 */
  private final String email;

  /** 사용자 유형 */
  private final UserType userType;

  /** 계정 상태 */
  private final AuthStatus status;

  // ========== 로그인 정보 ==========

  /** 로그인 실패 횟수 */
  private final int loginFailCount;

  /** 마지막 로그인 일시 */
  private final LocalDateTime lastLoginAt;

  // ========== 소셜 연동 정보 ==========

  /** 연동된 소셜 로그인 제공자 목록 */
  private final List<ProviderResponse> providers;

  // ========== 감사 정보 ==========

  /** 생성 일시 */
  private final LocalDateTime createdAt;

  /** 수정 일시 */
  private final LocalDateTime updatedAt;

  /** 삭제 일시 */
  private final LocalDateTime deletedAt;

  /**
   * Auth 엔티티를 응답 DTO로 변환한다.
   *
   * @param auth Auth 엔티티
   * @return Auth 응답 DTO
   */
  public static AuthResponse from(Auth auth) {
    return AuthResponse.builder()
        .id(auth.getId())
        .email(auth.getEmail())
        .userType(auth.getUserType())
        .status(auth.getStatus())
        .loginFailCount(auth.getLoginFailCount())
        .lastLoginAt(auth.getLastLoginAt())
        .providers(auth.getProviders().stream().map(ProviderResponse::from).toList())
        .createdAt(auth.getCreatedAt())
        .updatedAt(auth.getUpdatedAt())
        .deletedAt(auth.getDeletedAt())
        .build();
  }

  /** 소셜 로그인 제공자 응답 DTO. */
  @Getter
  @Builder
  public static class ProviderResponse {

    /** 제공자 ID */
    private final UUID id;

    /** 소셜 로그인 제공자 */
    private final ProviderType provider;

    /** 제공자 측 사용자 ID */
    private final String providerUserId;

    /** 연동 일시 */
    private final LocalDateTime connectedAt;

    public static ProviderResponse from(AuthProvider authProvider) {
      return ProviderResponse.builder()
          .id(authProvider.getId())
          .provider(authProvider.getProvider())
          .providerUserId(authProvider.getProviderUserId())
          .connectedAt(authProvider.getConnectedAt())
          .build();
    }
  }
}