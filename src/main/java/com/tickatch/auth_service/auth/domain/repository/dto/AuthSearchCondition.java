package com.tickatch.auth_service.auth.domain.repository.dto;

import com.tickatch.auth_service.auth.domain.vo.AuthStatus;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import lombok.Builder;
import lombok.Getter;

/**
 * Auth 검색 조건 DTO.
 *
 * <p>Auth 목록 조회 시 사용되는 검색 조건을 담는다. 모든 필드는 선택 사항이며, null인 경우 해당 조건을 적용하지 않는다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Getter
@Builder
public class AuthSearchCondition {

  /** 이메일 (부분 일치 검색) */
  private final String email;

  /** 사용자 유형 */
  private final UserType userType;

  /** 계정 상태 */
  private final AuthStatus status;

  /** 소셜 로그인 제공자 (해당 제공자로 연동된 계정만 조회) */
  private final ProviderType providerType;
}