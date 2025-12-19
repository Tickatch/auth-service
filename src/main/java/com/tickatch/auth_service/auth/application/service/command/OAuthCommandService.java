package com.tickatch.auth_service.auth.application.service.command;

import com.tickatch.auth_service.auth.application.messaging.AuthLogEventPublisher;
import com.tickatch.auth_service.auth.application.port.out.OAuthPort;
import com.tickatch.auth_service.auth.application.port.out.TokenPort;
import com.tickatch.auth_service.auth.application.service.command.dto.LoginResult;
import com.tickatch.auth_service.auth.domain.Auth;
import com.tickatch.auth_service.auth.domain.AuthRepository;
import com.tickatch.auth_service.auth.domain.exception.AuthErrorCode;
import com.tickatch.auth_service.auth.domain.exception.AuthException;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import com.tickatch.auth_service.auth.infrastructure.oauth.dto.OAuthState;
import com.tickatch.auth_service.auth.infrastructure.oauth.dto.OAuthUserInfo;
import com.tickatch.auth_service.token.application.service.command.dto.TokenResult;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth 인증 Command 서비스.
 *
 * <p>소셜 로그인, 계정 연동/해제를 담당한다. OAuth는 CUSTOMER 전용이다. 모든 주요 작업에 대해 성공/실패 로그를 로그 서비스로 발행한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OAuthCommandService {

  private final AuthRepository authRepository;
  private final OAuthPort oAuthPort;
  private final TokenPort tokenPort;
  private final PasswordEncoder passwordEncoder;
  private final AuthLogEventPublisher logEventPublisher;

  /**
   * OAuth 인증 URL을 생성한다.
   *
   * @param providerType 제공자 타입
   * @param rememberMe 로그인 유지 여부
   * @param deviceInfo 디바이스 정보
   * @return 인증 URL
   */
  public String getAuthorizationUrl(
      ProviderType providerType, boolean rememberMe, String deviceInfo) {
    if (!oAuthPort.isProviderConfigured(providerType)) {
      throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
    }

    OAuthState state = OAuthState.forLogin(rememberMe, deviceInfo);
    return oAuthPort.getAuthorizationUrl(providerType, state.encode());
  }

  /**
   * OAuth 콜백을 처리한다.
   *
   * <p>로그인 성공 시 OAUTH_LOGIN 또는 OAUTH_REGISTERED 로그를 발행한다.
   *
   * @param providerType 제공자 타입
   * @param code 인가 코드
   * @param encodedState 인코딩된 상태값
   * @return 로그인 결과
   */
  public LoginResult handleCallback(ProviderType providerType, String code, String encodedState) {
    OAuthState state = decodeAndValidateState(encodedState);

    if (state.isLinkRequest()) {
      return handleLinkCallback(providerType, code, state);
    }

    return handleLoginCallback(providerType, code, state);
  }

  /**
   * 계정 연동 URL을 생성한다.
   *
   * @param providerType 제공자 타입
   * @param authId 연동할 Auth ID
   * @param deviceInfo 디바이스 정보
   * @return 인증 URL
   */
  public String getLinkUrl(ProviderType providerType, UUID authId, String deviceInfo) {
    Auth auth =
        authRepository
            .findById(authId)
            .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_NOT_FOUND));

    if (!auth.getUserType().isCustomer()) {
      throw new AuthException(AuthErrorCode.OAUTH_NOT_ALLOWED_FOR_USER_TYPE);
    }

    if (auth.hasProvider(providerType)) {
      throw new AuthException(AuthErrorCode.PROVIDER_ALREADY_CONNECTED);
    }

    if (!oAuthPort.isProviderConfigured(providerType)) {
      throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_NOT_CONFIGURED);
    }

    OAuthState state = OAuthState.forLink(authId, deviceInfo);
    return oAuthPort.getAuthorizationUrl(providerType, state.encode());
  }

  /**
   * 소셜 계정 연동을 해제한다.
   *
   * <p>성공 시 PROVIDER_UNLINKED 로그를, 실패 시 PROVIDER_UNLINK_FAILED 로그를 발행한다.
   *
   * @param authId Auth ID
   * @param providerType 제공자 타입
   */
  public void unlinkProvider(UUID authId, ProviderType providerType) {
    String userType = "CUSTOMER";
    try {
      Auth auth =
          authRepository
              .findById(authId)
              .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_NOT_FOUND));

      userType = auth.getUserType().name();
      auth.disconnectProvider(providerType);
      log.info("소셜 계정 연동 해제 완료 - authId: {}, provider: {}", authId, providerType);

      logEventPublisher.publishProviderUnlinked(authId, userType);
    } catch (Exception e) {
      logEventPublisher.publishProviderUnlinkFailed(authId, userType);
      log.error(
          "소셜 계정 연동 해제 실패. authId: {}, provider: {}, error: {}",
          authId,
          providerType,
          e.getMessage(),
          e);
      throw e;
    }
  }

  /** 일반 로그인 콜백 처리. */
  private LoginResult handleLoginCallback(
      ProviderType providerType, String code, OAuthState state) {
    String userType = "CUSTOMER";
    try {
      OAuthUserInfo userInfo = oAuthPort.getUserInfo(providerType, code);

      if (userInfo.email() == null || userInfo.email().isBlank()) {
        log.warn("OAuth 이메일 정보 없음 - provider: {}", providerType);
        throw new AuthException(AuthErrorCode.OAUTH_EMAIL_REQUIRED);
      }

      // 기존 계정 확인 (제공자 정보로)
      Auth auth =
          authRepository
              .findByProviderAndProviderUserId(providerType, userInfo.providerUserId())
              .orElse(null);

      if (auth != null) {
        return loginExistingAccount(auth, state);
      }

      // 이메일로 기존 계정 확인 (CUSTOMER 타입)
      auth =
          authRepository.findByEmailAndUserType(userInfo.email(), UserType.CUSTOMER).orElse(null);

      if (auth != null) {
        auth.connectProvider(providerType, userInfo.providerUserId());
        log.info("기존 계정에 소셜 연동 - authId: {}, provider: {}", auth.getId(), providerType);
        logEventPublisher.publishProviderLinked(auth.getId(), userType);
        return loginExistingAccount(auth, state);
      }

      // 신규 회원가입
      return registerNewAccount(userInfo, state);
    } catch (Exception e) {
      logEventPublisher.publishOAuthLoginFailed(userType);
      log.error("OAuth 로그인 콜백 처리 실패. provider: {}, error: {}", providerType, e.getMessage(), e);
      throw e;
    }
  }

  /** 계정 연동 콜백 처리. */
  private LoginResult handleLinkCallback(ProviderType providerType, String code, OAuthState state) {
    String userType = "CUSTOMER";
    try {
      Auth auth =
          authRepository
              .findById(state.linkAuthId())
              .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_NOT_FOUND));

      userType = auth.getUserType().name();
      OAuthUserInfo userInfo = oAuthPort.getUserInfo(providerType, code);

      // 이미 다른 계정에 연동된 제공자인지 확인
      authRepository
          .findByProviderAndProviderUserId(providerType, userInfo.providerUserId())
          .ifPresent(
              existingAuth -> {
                if (!existingAuth.getId().equals(auth.getId())) {
                  log.warn("이미 다른 계정에 연동된 소셜 계정 - existingAuthId: {}", existingAuth.getId());
                  throw new AuthException(AuthErrorCode.PROVIDER_ALREADY_CONNECTED);
                }
              });

      auth.connectProvider(providerType, userInfo.providerUserId());
      log.info("소셜 계정 연동 완료 - authId: {}, provider: {}", auth.getId(), providerType);

      logEventPublisher.publishProviderLinked(auth.getId(), userType);

      TokenResult tokenResult =
          tokenPort.issueTokens(auth.getId(), auth.getUserType(), state.deviceInfo(), false);

      return LoginResult.of(auth.getId(), auth.getEmail(), auth.getUserType(), tokenResult);
    } catch (Exception e) {
      logEventPublisher.publishProviderLinkFailed(state.linkAuthId(), userType);
      log.error(
          "소셜 계정 연동 콜백 처리 실패. authId: {}, provider: {}, error: {}",
          state.linkAuthId(),
          providerType,
          e.getMessage(),
          e);
      throw e;
    }
  }

  /** 기존 계정으로 로그인. */
  private LoginResult loginExistingAccount(Auth auth, OAuthState state) {
    String userType = auth.getUserType().name();
    auth.recordLoginSuccess();
    log.info("OAuth 로그인 성공 - authId: {}", auth.getId());

    TokenResult tokenResult =
        tokenPort.issueTokens(
            auth.getId(), auth.getUserType(), state.deviceInfo(), state.rememberMe());

    logEventPublisher.publishOAuthLogin(auth.getId(), userType);
    return LoginResult.of(auth.getId(), auth.getEmail(), auth.getUserType(), tokenResult);
  }

  /** 신규 회원가입. */
  private LoginResult registerNewAccount(OAuthUserInfo userInfo, OAuthState state) {
    String userType = "CUSTOMER";
    String tempPassword = UUID.randomUUID().toString();

    Auth auth =
        Auth.registerWithOAuth(
            userInfo.email(),
            tempPassword,
            UserType.CUSTOMER,
            userInfo.providerType(),
            userInfo.providerUserId(),
            passwordEncoder,
            "OAUTH");

    authRepository.save(auth);
    log.info(
        "OAuth 회원가입 완료 - authId: {}, email: {}, provider: {}",
        auth.getId(),
        auth.getEmail(),
        userInfo.providerType());

    TokenResult tokenResult =
        tokenPort.issueTokens(
            auth.getId(), auth.getUserType(), state.deviceInfo(), state.rememberMe());

    logEventPublisher.publishOAuthRegistered(auth.getId(), userType);
    return LoginResult.of(auth.getId(), auth.getEmail(), auth.getUserType(), tokenResult);
  }

  /** 상태값 디코딩 및 검증. */
  private OAuthState decodeAndValidateState(String encodedState) {
    if (encodedState == null || encodedState.isBlank()) {
      throw new AuthException(AuthErrorCode.INVALID_OAUTH_STATE);
    }

    try {
      return OAuthState.decode(encodedState);
    } catch (Exception e) {
      log.warn("OAuth 상태값 디코딩 실패 - state: {}", encodedState, e);
      throw new AuthException(AuthErrorCode.INVALID_OAUTH_STATE);
    }
  }
}
