package com.tickatch.auth_service.token.domain.repository.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/**
 * RefreshToken 검색 조건 DTO.
 *
 * <p>RefreshToken 목록 조회 시 사용되는 검색 조건을 담는다. 모든 필드는 선택 사항이며, null인 경우 해당 조건을 적용하지 않는다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Getter
@Builder
public class RefreshTokenSearchCondition {

  /** Auth ID */
  private final UUID authId;

  /** 폐기 여부 (true: 폐기됨, false: 유효, null: 전체) */
  private final Boolean revoked;

  /** 만료 여부 (true: 만료됨, false: 유효, null: 전체) */
  private final Boolean expired;

  /** 로그인 유지 여부 (true: 로그인 유지, false: 일반, null: 전체) */
  private final Boolean rememberMe;

  /** 디바이스 정보 (부분 일치 검색) */
  private final String deviceInfo;
}
