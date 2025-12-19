package com.tickatch.auth_service.auth.infrastructure.messaging.publisher;

import com.tickatch.auth_service.auth.application.messaging.AuthLogEventPublisher;
import com.tickatch.auth_service.auth.infrastructure.messaging.config.RabbitMQConfig;
import com.tickatch.auth_service.auth.infrastructure.messaging.event.AuthActionType;
import com.tickatch.auth_service.auth.infrastructure.messaging.event.AuthLogEvent;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * RabbitMQ 기반 인증 로그 이벤트 발행자.
 *
 * <p>Auth Service에서 발생하는 주요 액션에 대한 로그 이벤트를 RabbitMQ를 통해 로그 서비스로 발행한다. 로그 발행 실패 시에도 비즈니스 로직에 영향을 주지
 * 않도록 예외를 던지지 않고 에러 로그로 기록한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RabbitAuthLogPublisher implements AuthLogEventPublisher {

  private final RabbitTemplate rabbitTemplate;

  @Value("${messaging.exchange.log:tickatch.log}")
  private String logExchange;

  // ========================================
  // 회원가입 관련
  // ========================================

  @Override
  public void publishRegistered(UUID authId, String userType) {
    publish(authId, userType, AuthActionType.REGISTERED);
    log.info("회원가입 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  @Override
  public void publishRegisterFailed(String userType) {
    publish(null, userType, AuthActionType.REGISTER_FAILED);
    log.warn("회원가입 실패 로그 발행. userType: {}", userType);
  }

  @Override
  public void publishOAuthRegistered(UUID authId, String userType) {
    publish(authId, userType, AuthActionType.OAUTH_REGISTERED);
    log.info("OAuth 회원가입 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  @Override
  public void publishOAuthRegisterFailed(String userType) {
    publish(null, userType, AuthActionType.OAUTH_REGISTER_FAILED);
    log.warn("OAuth 회원가입 실패 로그 발행. userType: {}", userType);
  }

  // ========================================
  // 로그인/로그아웃 관련
  // ========================================

  @Override
  public void publishLogin(UUID authId, String userType) {
    publish(authId, userType, AuthActionType.LOGIN);
    log.info("로그인 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  @Override
  public void publishLoginFailed(String userType) {
    publish(null, userType, AuthActionType.LOGIN_FAILED);
    log.warn("로그인 실패 로그 발행. userType: {}", userType);
  }

  @Override
  public void publishOAuthLogin(UUID authId, String userType) {
    publish(authId, userType, AuthActionType.OAUTH_LOGIN);
    log.info("OAuth 로그인 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  @Override
  public void publishOAuthLoginFailed(String userType) {
    publish(null, userType, AuthActionType.OAUTH_LOGIN_FAILED);
    log.warn("OAuth 로그인 실패 로그 발행. userType: {}", userType);
  }

  @Override
  public void publishLogout(UUID authId, String userType) {
    publish(authId, userType, AuthActionType.LOGOUT);
    log.info("로그아웃 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  @Override
  public void publishLogoutFailed(UUID authId, String userType) {
    publish(authId, userType, AuthActionType.LOGOUT_FAILED);
    log.warn("로그아웃 실패 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  // ========================================
  // 토큰 관련
  // ========================================

  @Override
  public void publishTokenRefreshed(UUID authId, String userType) {
    publish(authId, userType, AuthActionType.TOKEN_REFRESHED);
    log.debug("토큰 갱신 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  @Override
  public void publishTokenRefreshFailed(UUID authId, String userType) {
    publish(authId, userType, AuthActionType.TOKEN_REFRESH_FAILED);
    log.warn("토큰 갱신 실패 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  // ========================================
  // 비밀번호 관련
  // ========================================

  @Override
  public void publishPasswordChanged(UUID authId, String userType) {
    publish(authId, userType, AuthActionType.PASSWORD_CHANGED);
    log.info("비밀번호 변경 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  @Override
  public void publishPasswordChangeFailed(UUID authId, String userType) {
    publish(authId, userType, AuthActionType.PASSWORD_CHANGE_FAILED);
    log.warn("비밀번호 변경 실패 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  // ========================================
  // 탈퇴 관련
  // ========================================

  @Override
  public void publishWithdrawn(UUID authId, String userType) {
    publish(authId, userType, AuthActionType.WITHDRAWN);
    log.info("탈퇴 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  @Override
  public void publishWithdrawFailed(UUID authId, String userType) {
    publish(authId, userType, AuthActionType.WITHDRAW_FAILED);
    log.warn("탈퇴 실패 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  // ========================================
  // 이벤트 기반 상태 동기화
  // ========================================

  @Override
  public void publishUserWithdrawnSynced(UUID authId, String userType) {
    publishSystemEvent(authId, userType, AuthActionType.USER_WITHDRAWN_SYNCED);
    log.info("사용자 탈퇴 동기화 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  @Override
  public void publishUserSuspendedSynced(UUID authId, String userType) {
    publishSystemEvent(authId, userType, AuthActionType.USER_SUSPENDED_SYNCED);
    log.info("사용자 정지 동기화 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  @Override
  public void publishUserActivatedSynced(UUID authId, String userType) {
    publishSystemEvent(authId, userType, AuthActionType.USER_ACTIVATED_SYNCED);
    log.info("사용자 활성화 동기화 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  // ========================================
  // OAuth 계정 연동 관련
  // ========================================

  @Override
  public void publishProviderLinked(UUID authId, String userType) {
    publish(authId, userType, AuthActionType.PROVIDER_LINKED);
    log.info("소셜 계정 연동 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  @Override
  public void publishProviderLinkFailed(UUID authId, String userType) {
    publish(authId, userType, AuthActionType.PROVIDER_LINK_FAILED);
    log.warn("소셜 계정 연동 실패 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  @Override
  public void publishProviderUnlinked(UUID authId, String userType) {
    publish(authId, userType, AuthActionType.PROVIDER_UNLINKED);
    log.info("소셜 계정 연동 해제 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  @Override
  public void publishProviderUnlinkFailed(UUID authId, String userType) {
    publish(authId, userType, AuthActionType.PROVIDER_UNLINK_FAILED);
    log.warn("소셜 계정 연동 해제 실패 로그 발행. authId: {}, userType: {}", authId, userType);
  }

  // ========================================
  // Private Methods
  // ========================================

  private void publish(UUID authId, String userType, String actionType) {
    try {
      AuthLogEvent event = AuthLogEvent.create(authId, userType, actionType);
      rabbitTemplate.convertAndSend(logExchange, RabbitMQConfig.ROUTING_KEY_AUTH_LOG, event);
      log.debug(
          "인증 로그 이벤트 발행 완료. eventId: {}, authId: {}, actionType: {}",
          event.eventId(),
          authId,
          actionType);
    } catch (Exception e) {
      log.error(
          "인증 로그 이벤트 발행 실패. authId: {}, actionType: {}, error: {}",
          authId,
          actionType,
          e.getMessage(),
          e);
    }
  }

  private void publishSystemEvent(UUID authId, String userType, String actionType) {
    try {
      AuthLogEvent event = AuthLogEvent.createSystemEvent(authId, userType, actionType);
      rabbitTemplate.convertAndSend(logExchange, RabbitMQConfig.ROUTING_KEY_AUTH_LOG, event);
      log.debug(
          "인증 시스템 로그 이벤트 발행 완료. eventId: {}, authId: {}, actionType: {}",
          event.eventId(),
          authId,
          actionType);
    } catch (Exception e) {
      log.error(
          "인증 시스템 로그 이벤트 발행 실패. authId: {}, actionType: {}, error: {}",
          authId,
          actionType,
          e.getMessage(),
          e);
    }
  }
}
