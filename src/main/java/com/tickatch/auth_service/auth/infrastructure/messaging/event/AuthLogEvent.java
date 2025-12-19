package com.tickatch.auth_service.auth.infrastructure.messaging.event;

import com.tickatch.auth_service.global.config.ActorExtractor;
import com.tickatch.auth_service.global.config.ActorExtractor.ActorInfo;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 인증 로그 이벤트.
 *
 * <p>Auth Service에서 발생하는 주요 액션에 대한 로그 정보를 담는 이벤트 객체이다. 로그 서비스로 전송되어 인증 관련 활동 이력을 기록한다.
 *
 * <p>이벤트 정보:
 *
 * <ul>
 *   <li>Exchange: tickatch.log
 *   <li>Routing Key: auth.log
 *   <li>대상 서비스: log-service
 * </ul>
 *
 * @param eventId 이벤트 고유 ID
 * @param authId 대상 Auth ID
 * @param userType 사용자 유형 (CUSTOMER, SELLER, ADMIN)
 * @param actionType 액션 타입 ({@link AuthActionType} 참조)
 * @param actorType 액터 타입 (ADMIN, SELLER, CUSTOMER, SYSTEM)
 * @param actorUserId 액터 사용자 ID (SYSTEM인 경우 null)
 * @param occurredAt 이벤트 발생 시간
 * @author Tickatch
 * @since 1.0.0
 */
public record AuthLogEvent(
    UUID eventId,
    UUID authId,
    String userType,
    String actionType,
    String actorType,
    UUID actorUserId,
    LocalDateTime occurredAt) {

  /**
   * 새로운 인증 로그 이벤트를 생성한다.
   *
   * <p>ActorInfo는 {@link ActorExtractor}를 통해 SecurityContext에서 자동 추출된다.
   *
   * @param authId 대상 Auth ID
   * @param userType 사용자 유형
   * @param actionType 액션 타입
   * @return 생성된 AuthLogEvent
   */
  public static AuthLogEvent create(UUID authId, String userType, String actionType) {
    ActorInfo actorInfo = ActorExtractor.extract();
    return new AuthLogEvent(
        UUID.randomUUID(),
        authId,
        userType,
        actionType,
        actorInfo.actorType(),
        actorInfo.actorUserId(),
        LocalDateTime.now());
  }

  /**
   * 시스템에 의한 인증 로그 이벤트를 생성한다.
   *
   * @param authId 대상 Auth ID
   * @param userType 사용자 유형
   * @param actionType 액션 타입
   * @return 생성된 AuthLogEvent
   */
  public static AuthLogEvent createSystemEvent(UUID authId, String userType, String actionType) {
    return new AuthLogEvent(
        UUID.randomUUID(), authId, userType, actionType, "SYSTEM", null, LocalDateTime.now());
  }
}
