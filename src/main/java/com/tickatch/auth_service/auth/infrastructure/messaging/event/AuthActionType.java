package com.tickatch.auth_service.auth.infrastructure.messaging.event;

/**
 * 인증 로그 액션 타입.
 *
 * <p>Auth Service에서 발생할 수 있는 모든 액션 타입을 정의한다. 로그 서비스에서 이 값을 기준으로 액션을 분류하고 저장한다.
 *
 * <p>카테고리별 액션:
 *
 * <ul>
 *   <li>회원가입: REGISTERED, REGISTER_FAILED, OAUTH_REGISTERED, OAUTH_REGISTER_FAILED
 *   <li>로그인/로그아웃: LOGIN, LOGIN_FAILED, OAUTH_LOGIN, OAUTH_LOGIN_FAILED, LOGOUT
 *   <li>토큰: TOKEN_REFRESHED, TOKEN_REFRESH_FAILED
 *   <li>비밀번호: PASSWORD_CHANGED, PASSWORD_CHANGE_FAILED
 *   <li>탈퇴: WITHDRAWN, WITHDRAW_FAILED
 *   <li>상태 동기화: USER_WITHDRAWN_SYNCED, USER_SUSPENDED_SYNCED, USER_ACTIVATED_SYNCED
 *   <li>OAuth: PROVIDER_LINKED, PROVIDER_UNLINKED
 * </ul>
 *
 * @author Tickatch
 * @since 1.0.0
 * @see AuthLogEvent
 */
public final class AuthActionType {

  private AuthActionType() {
    // 인스턴스화 방지
  }

  // ========================================
  // 회원가입 관련
  // ========================================

  /** 회원가입 성공 */
  public static final String REGISTERED = "REGISTERED";

  /** 회원가입 실패 */
  public static final String REGISTER_FAILED = "REGISTER_FAILED";

  /** OAuth 회원가입 성공 */
  public static final String OAUTH_REGISTERED = "OAUTH_REGISTERED";

  /** OAuth 회원가입 실패 */
  public static final String OAUTH_REGISTER_FAILED = "OAUTH_REGISTER_FAILED";

  // ========================================
  // 로그인/로그아웃 관련
  // ========================================

  /** 로그인 성공 */
  public static final String LOGIN = "LOGIN";

  /** 로그인 실패 */
  public static final String LOGIN_FAILED = "LOGIN_FAILED";

  /** OAuth 로그인 성공 */
  public static final String OAUTH_LOGIN = "OAUTH_LOGIN";

  /** OAuth 로그인 실패 */
  public static final String OAUTH_LOGIN_FAILED = "OAUTH_LOGIN_FAILED";

  /** 로그아웃 */
  public static final String LOGOUT = "LOGOUT";

  /** 로그아웃 실패 */
  public static final String LOGOUT_FAILED = "LOGOUT_FAILED";

  // ========================================
  // 토큰 관련
  // ========================================

  /** 토큰 갱신 성공 */
  public static final String TOKEN_REFRESHED = "TOKEN_REFRESHED";

  /** 토큰 갱신 실패 */
  public static final String TOKEN_REFRESH_FAILED = "TOKEN_REFRESH_FAILED";

  // ========================================
  // 비밀번호 관련
  // ========================================

  /** 비밀번호 변경 성공 */
  public static final String PASSWORD_CHANGED = "PASSWORD_CHANGED";

  /** 비밀번호 변경 실패 */
  public static final String PASSWORD_CHANGE_FAILED = "PASSWORD_CHANGE_FAILED";

  // ========================================
  // 탈퇴 관련
  // ========================================

  /** 탈퇴 성공 */
  public static final String WITHDRAWN = "WITHDRAWN";

  /** 탈퇴 실패 */
  public static final String WITHDRAW_FAILED = "WITHDRAW_FAILED";

  // ========================================
  // 이벤트 기반 상태 동기화 (User Service → Auth Service)
  // ========================================

  /** 사용자 탈퇴 이벤트 동기화 완료 */
  public static final String USER_WITHDRAWN_SYNCED = "USER_WITHDRAWN_SYNCED";

  /** 사용자 정지 이벤트 동기화 완료 */
  public static final String USER_SUSPENDED_SYNCED = "USER_SUSPENDED_SYNCED";

  /** 사용자 활성화 이벤트 동기화 완료 */
  public static final String USER_ACTIVATED_SYNCED = "USER_ACTIVATED_SYNCED";

  // ========================================
  // OAuth 계정 연동 관련
  // ========================================

  /** 소셜 계정 연동 성공 */
  public static final String PROVIDER_LINKED = "PROVIDER_LINKED";

  /** 소셜 계정 연동 실패 */
  public static final String PROVIDER_LINK_FAILED = "PROVIDER_LINK_FAILED";

  /** 소셜 계정 연동 해제 성공 */
  public static final String PROVIDER_UNLINKED = "PROVIDER_UNLINKED";

  /** 소셜 계정 연동 해제 실패 */
  public static final String PROVIDER_UNLINK_FAILED = "PROVIDER_UNLINK_FAILED";
}
