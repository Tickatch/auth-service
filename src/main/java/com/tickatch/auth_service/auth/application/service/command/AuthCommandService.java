package com.tickatch.auth_service.auth.application.service.command;

import com.tickatch.auth_service.auth.application.port.out.TokenPort;
import com.tickatch.auth_service.auth.application.service.command.dto.ChangePasswordCommand;
import com.tickatch.auth_service.auth.application.service.command.dto.LoginCommand;
import com.tickatch.auth_service.auth.application.service.command.dto.LoginResult;
import com.tickatch.auth_service.auth.application.service.command.dto.LogoutCommand;
import com.tickatch.auth_service.auth.application.service.command.dto.OAuthLoginCommand;
import com.tickatch.auth_service.auth.application.service.command.dto.OAuthRegisterCommand;
import com.tickatch.auth_service.auth.application.service.command.dto.RefreshCommand;
import com.tickatch.auth_service.auth.application.service.command.dto.RegisterCommand;
import com.tickatch.auth_service.auth.domain.Auth;
import com.tickatch.auth_service.auth.domain.AuthRepository;
import com.tickatch.auth_service.auth.domain.exception.AuthErrorCode;
import com.tickatch.auth_service.auth.domain.exception.AuthException;
import com.tickatch.auth_service.token.application.service.command.dto.TokenResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 관련 Command 서비스.
 *
 * <p>회원가입, 로그인, 로그아웃, 비밀번호 변경 등 인증 관련 커맨드를 처리한다.
 *
 * <p>탈퇴는 User Service를 통해서만 처리되며, 이 서비스는 User Service에서 발행한
 * 이벤트를 수신하여 Auth 상태를 동기화한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthCommandService {

  private final AuthRepository authRepository;
  private final TokenPort tokenPort;
  private final PasswordEncoder passwordEncoder;

  // ========================================
  // 회원가입
  // ========================================

  /**
   * 회원가입을 처리한다.
   *
   * @param command 회원가입 요청
   * @return 로그인 결과 (토큰 포함)
   * @throws AuthException 이메일 중복 시
   */
  public LoginResult register(RegisterCommand command) {
    validateEmailNotDuplicate(command.email(), command.userType());

    Auth auth = Auth.register(
        command.email(),
        command.password(),
        command.userType(),
        passwordEncoder,
        "SYSTEM"
    );

    authRepository.save(auth);
    log.info("회원가입 완료 - authId: {}, email: {}, userType: {}",
        auth.getId(), auth.getEmail(), auth.getUserType());

    TokenResult tokenResult = tokenPort.issueTokens(
        auth.getId(),
        auth.getUserType(),
        command.deviceInfo(),
        command.rememberMe()
    );

    return LoginResult.of(auth.getId(), auth.getEmail(), auth.getUserType(), tokenResult);
  }

  /**
   * 소셜 로그인으로 회원가입을 처리한다.
   *
   * @param command 소셜 회원가입 요청
   * @return 로그인 결과 (토큰 포함)
   * @throws AuthException 이메일 중복 또는 CUSTOMER가 아닌 경우
   */
  public LoginResult registerWithOAuth(OAuthRegisterCommand command) {
    validateEmailNotDuplicate(command.email(), command.userType());

    Auth auth = Auth.registerWithOAuth(
        command.email(),
        command.password(),
        command.userType(),
        command.providerType(),
        command.providerUserId(),
        passwordEncoder,
        "SYSTEM"
    );

    authRepository.save(auth);
    log.info("소셜 회원가입 완료 - authId: {}, email: {}, provider: {}",
        auth.getId(), auth.getEmail(), command.providerType());

    TokenResult tokenResult = tokenPort.issueTokens(
        auth.getId(),
        auth.getUserType(),
        command.deviceInfo(),
        command.rememberMe()
    );

    return LoginResult.of(auth.getId(), auth.getEmail(), auth.getUserType(), tokenResult);
  }

  // ========================================
  // 로그인/로그아웃
  // ========================================

  /**
   * 로그인을 처리한다.
   *
   * @param command 로그인 요청
   * @return 로그인 결과 (토큰 포함)
   * @throws AuthException 인증 실패 시
   */
  public LoginResult login(LoginCommand command) {
    Auth auth = authRepository.findByEmailAndUserType(command.email(), command.userType())
        .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_CREDENTIALS));

    if (!auth.matchesPassword(command.password(), passwordEncoder)) {
      auth.recordLoginFailure();
      log.warn("로그인 실패 - email: {}, failCount: {}", command.email(), auth.getLoginFailCount());
      throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
    }

    auth.recordLoginSuccess();
    log.info("로그인 성공 - authId: {}, email: {}", auth.getId(), auth.getEmail());

    TokenResult tokenResult = tokenPort.issueTokens(
        auth.getId(),
        auth.getUserType(),
        command.deviceInfo(),
        command.rememberMe()
    );

    return LoginResult.of(auth.getId(), auth.getEmail(), auth.getUserType(), tokenResult);
  }

  /**
   * 소셜 로그인을 처리한다.
   *
   * @param command 소셜 로그인 요청
   * @return 로그인 결과 (토큰 포함)
   * @throws AuthException 계정을 찾을 수 없는 경우
   */
  public LoginResult loginWithOAuth(OAuthLoginCommand command) {
    Auth auth = authRepository.findByProviderAndProviderUserId(command.providerType(), command.providerUserId())
        .orElseThrow(() -> new AuthException(AuthErrorCode.SOCIAL_ACCOUNT_NOT_FOUND));

    if (auth.getUserType() != command.userType()) {
      throw new AuthException(AuthErrorCode.USER_TYPE_MISMATCH);
    }

    auth.recordLoginSuccess();
    log.info("소셜 로그인 성공 - authId: {}, provider: {}", auth.getId(), command.providerType());

    TokenResult tokenResult = tokenPort.issueTokens(
        auth.getId(),
        auth.getUserType(),
        command.deviceInfo(),
        command.rememberMe()
    );

    return LoginResult.of(auth.getId(), auth.getEmail(), auth.getUserType(), tokenResult);
  }

  /**
   * 토큰을 갱신한다.
   *
   * @param command 토큰 갱신 요청
   * @return 갱신된 토큰 정보
   */
  public LoginResult refresh(RefreshCommand command) {
    UUID authId = tokenPort.getAuthIdFromRefreshToken(command.refreshToken());

    Auth auth = authRepository.findById(authId)
        .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_NOT_FOUND));

    TokenResult tokenResult = tokenPort.refreshTokens(command.refreshToken(), auth.getUserType());

    return LoginResult.of(auth.getId(), auth.getEmail(), auth.getUserType(), tokenResult);
  }

  /**
   * 로그아웃을 처리한다.
   *
   * @param command 로그아웃 요청
   */
  public void logout(LogoutCommand command) {
    if (command.allDevices()) {
      tokenPort.revokeAllTokens(command.authId());
      log.info("전체 로그아웃 완료 - authId: {}", command.authId());
    } else {
      tokenPort.revokeToken(command.refreshToken());
      log.info("로그아웃 완료 - authId: {}", command.authId());
    }
  }

  // ========================================
  // 비밀번호 변경
  // ========================================

  /**
   * 비밀번호를 변경한다.
   *
   * @param command 비밀번호 변경 요청
   * @throws AuthException 현재 비밀번호가 일치하지 않는 경우
   */
  public void changePassword(ChangePasswordCommand command) {
    Auth auth = authRepository.findById(command.authId())
        .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_NOT_FOUND));

    if (!auth.matchesPassword(command.currentPassword(), passwordEncoder)) {
      throw new AuthException(AuthErrorCode.INVALID_CURRENT_PASSWORD);
    }

    auth.changePassword(command.newPassword(), passwordEncoder, auth.getId().toString());
    log.info("비밀번호 변경 완료 - authId: {}", command.authId());

    tokenPort.revokeAllTokens(command.authId());
  }

  // ========================================
  // 이벤트 기반 상태 동기화 (User Service → Auth Service)
  // ========================================

  /**
   * 사용자 탈퇴 이벤트를 처리한다.
   *
   * <p>User Service에서 발행한 탈퇴 이벤트를 수신하여 Auth 상태를 WITHDRAWN으로 변경하고
   * 모든 토큰을 삭제한다.
   *
   * <p>Auth 계정을 찾을 수 없는 경우 경고 로그만 남기고 정상 종료한다.
   *
   * @param authId 탈퇴할 Auth ID
   */
  public void handleUserWithdrawn(UUID authId) {
    authRepository.findById(authId).ifPresentOrElse(
        auth -> {
          auth.withdraw(authId.toString());
          tokenPort.deleteAllTokens(authId);
          log.info("탈퇴 이벤트 처리 완료 - authId: {}", authId);
        },
        () -> log.warn("탈퇴 이벤트 처리 실패: Auth를 찾을 수 없음 - authId: {}", authId)
    );
  }

  /**
   * 사용자 정지 이벤트를 처리한다.
   *
   * <p>User Service에서 발행한 정지 이벤트를 수신하여 Auth 상태를 LOCKED로 변경하고
   * 모든 토큰을 무효화한다. 이후 해당 사용자는 로그인이 불가능해진다.
   *
   * <p>Auth 계정을 찾을 수 없는 경우 경고 로그만 남기고 정상 종료한다.
   *
   * @param authId 정지할 Auth ID
   */
  public void handleUserSuspended(UUID authId) {
    authRepository.findById(authId).ifPresentOrElse(
        auth -> {
          auth.lock();
          tokenPort.revokeAllTokens(authId);
          log.info("정지 이벤트 처리 완료 - authId: {}", authId);
        },
        () -> log.warn("정지 이벤트 처리 실패: Auth를 찾을 수 없음 - authId: {}", authId)
    );
  }

  /**
   * 사용자 활성화 이벤트를 처리한다.
   *
   * <p>User Service에서 발행한 활성화 이벤트를 수신하여 Auth 상태를 ACTIVE로 변경한다.
   * 이후 해당 사용자는 다시 로그인이 가능해진다.
   *
   * <p>Auth 계정을 찾을 수 없는 경우 경고 로그만 남기고 정상 종료한다.
   *
   * @param authId 활성화할 Auth ID
   */
  public void handleUserActivated(UUID authId) {
    authRepository.findById(authId).ifPresentOrElse(
        auth -> {
          auth.unlock();
          log.info("활성화 이벤트 처리 완료 - authId: {}", authId);
        },
        () -> log.warn("활성화 이벤트 처리 실패: Auth를 찾을 수 없음 - authId: {}", authId)
    );
  }

  // ========================================
  // 내부 메서드
  // ========================================

  private void validateEmailNotDuplicate(String email,
      com.tickatch.auth_service.auth.domain.vo.UserType userType) {
    if (authRepository.existsByEmailAndUserType(email, userType)) {
      throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
    }
  }
}