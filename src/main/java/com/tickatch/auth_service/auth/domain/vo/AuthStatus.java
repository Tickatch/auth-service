package com.tickatch.auth_service.auth.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 인증 계정 상태.
 *
 * <p>상태 전이 규칙:
 *
 * <pre>
 * ACTIVE ──→ LOCKED ──→ ACTIVE (비밀번호 초기화)
 *    │
 *    ↓
 * WITHDRAWN (최종 상태)
 * </pre>
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum AuthStatus {

  /** 활성 상태 */
  ACTIVE("활성"),

  /** 잠금 상태 (로그인 실패 초과) */
  LOCKED("잠금"),

  /** 탈퇴 상태 */
  WITHDRAWN("탈퇴");

  /** 상태 설명 */
  private final String description;

  /**
   * 대상 상태로 전이 가능한지 확인한다.
   *
   * @param target 대상 상태
   * @return 전이 가능하면 true
   */
  public boolean canChangeTo(AuthStatus target) {
    if (target == null || this == target) {
      return false;
    }

    return switch (this) {
      case ACTIVE -> target == LOCKED || target == WITHDRAWN;
      case LOCKED -> target == ACTIVE;
      case WITHDRAWN -> false;
    };
  }

  /**
   * 로그인 가능한 상태인지 확인한다.
   *
   * @return ACTIVE 상태이면 true
   */
  public boolean canLogin() {
    return this == ACTIVE;
  }

  /**
   * 활성 상태인지 확인한다.
   *
   * @return ACTIVE 상태이면 true
   */
  public boolean isActive() {
    return this == ACTIVE;
  }

  /**
   * 잠금 상태인지 확인한다.
   *
   * @return LOCKED 상태이면 true
   */
  public boolean isLocked() {
    return this == LOCKED;
  }

  /**
   * 탈퇴 상태인지 확인한다.
   *
   * @return WITHDRAWN 상태이면 true
   */
  public boolean isWithdrawn() {
    return this == WITHDRAWN;
  }

  /**
   * 최종 상태인지 확인한다.
   *
   * @return WITHDRAWN 상태이면 true
   */
  public boolean isTerminal() {
    return this == WITHDRAWN;
  }
}
