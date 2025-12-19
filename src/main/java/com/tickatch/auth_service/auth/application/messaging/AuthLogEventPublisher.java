package com.tickatch.auth_service.auth.application.messaging;

import java.util.UUID;

/**
 * 인증 로그 이벤트 발행 인터페이스.
 *
 * <p>Application 레이어에서 정의하고, Infrastructure 레이어에서 구현한다. 이를 통해 Application은 메시징 기술(RabbitMQ, Kafka
 * 등)에 의존하지 않는다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
public interface AuthLogEventPublisher {

  // ========================================
  // 회원가입 관련
  // ========================================

  /**
   * 회원가입 성공 로그를 발행한다.
   *
   * @param authId 생성된 Auth ID
   * @param userType 사용자 유형
   */
  void publishRegistered(UUID authId, String userType);

  /**
   * 회원가입 실패 로그를 발행한다.
   *
   * @param userType 사용자 유형
   */
  void publishRegisterFailed(String userType);

  /**
   * OAuth 회원가입 성공 로그를 발행한다.
   *
   * @param authId 생성된 Auth ID
   * @param userType 사용자 유형
   */
  void publishOAuthRegistered(UUID authId, String userType);

  /**
   * OAuth 회원가입 실패 로그를 발행한다.
   *
   * @param userType 사용자 유형
   */
  void publishOAuthRegisterFailed(String userType);

  // ========================================
  // 로그인/로그아웃 관련
  // ========================================

  /**
   * 로그인 성공 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   */
  void publishLogin(UUID authId, String userType);

  /**
   * 로그인 실패 로그를 발행한다.
   *
   * @param userType 사용자 유형
   */
  void publishLoginFailed(String userType);

  /**
   * OAuth 로그인 성공 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   */
  void publishOAuthLogin(UUID authId, String userType);

  /**
   * OAuth 로그인 실패 로그를 발행한다.
   *
   * @param userType 사용자 유형
   */
  void publishOAuthLoginFailed(String userType);

  /**
   * 로그아웃 성공 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   */
  void publishLogout(UUID authId, String userType);

  /**
   * 로그아웃 실패 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   */
  void publishLogoutFailed(UUID authId, String userType);

  // ========================================
  // 토큰 관련
  // ========================================

  /**
   * 토큰 갱신 성공 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   */
  void publishTokenRefreshed(UUID authId, String userType);

  /**
   * 토큰 갱신 실패 로그를 발행한다.
   *
   * @param authId Auth ID (null일 수 있음)
   * @param userType 사용자 유형
   */
  void publishTokenRefreshFailed(UUID authId, String userType);

  // ========================================
  // 비밀번호 관련
  // ========================================

  /**
   * 비밀번호 변경 성공 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   */
  void publishPasswordChanged(UUID authId, String userType);

  /**
   * 비밀번호 변경 실패 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   */
  void publishPasswordChangeFailed(UUID authId, String userType);

  // ========================================
  // 탈퇴 관련
  // ========================================

  /**
   * 탈퇴 성공 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   */
  void publishWithdrawn(UUID authId, String userType);

  /**
   * 탈퇴 실패 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   */
  void publishWithdrawFailed(UUID authId, String userType);

  // ========================================
  // 이벤트 기반 상태 동기화 (User Service → Auth Service)
  // ========================================

  /**
   * 사용자 탈퇴 이벤트 동기화 완료 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   */
  void publishUserWithdrawnSynced(UUID authId, String userType);

  /**
   * 사용자 정지 이벤트 동기화 완료 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   */
  void publishUserSuspendedSynced(UUID authId, String userType);

  /**
   * 사용자 활성화 이벤트 동기화 완료 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   */
  void publishUserActivatedSynced(UUID authId, String userType);

  // ========================================
  // OAuth 계정 연동 관련
  // ========================================

  /**
   * 소셜 계정 연동 성공 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   */
  void publishProviderLinked(UUID authId, String userType);

  /**
   * 소셜 계정 연동 실패 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   */
  void publishProviderLinkFailed(UUID authId, String userType);

  /**
   * 소셜 계정 연동 해제 성공 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   */
  void publishProviderUnlinked(UUID authId, String userType);

  /**
   * 소셜 계정 연동 해제 실패 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param userType 사용자 유형
   */
  void publishProviderUnlinkFailed(UUID authId, String userType);
}
