package com.tickatch.auth_service.auth.infrastructure.messaging.consumer;

import com.tickatch.auth_service.auth.application.service.command.AuthCommandService;
import com.tickatch.auth_service.auth.infrastructure.messaging.config.RabbitMQConfig;
import com.tickatch.auth_service.auth.infrastructure.messaging.event.UserStatusChangedEvent;
import io.github.tickatch.common.event.EventContext;
import io.github.tickatch.common.event.IntegrationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 사용자 상태 변경 이벤트 Consumer.
 *
 * <p>User Service에서 발행하는 사용자 상태 변경 이벤트를 수신하여 Auth 상태를 동기화한다.
 *
 * <p>처리하는 이벤트:
 * <ul>
 *   <li>탈퇴 (WITHDRAWN): Auth 상태를 WITHDRAWN으로 변경, 모든 토큰 삭제
 *   <li>정지 (SUSPENDED): Auth 상태를 LOCKED로 변경, 모든 토큰 무효화
 *   <li>활성화 (ACTIVATED): Auth 상태를 ACTIVE로 변경
 * </ul>
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

  private final AuthCommandService authCommandService;

  /**
   * 사용자 탈퇴 이벤트를 수신하여 처리한다.
   *
   * @param integrationEvent IntegrationEvent
   */
  @RabbitListener(queues = RabbitMQConfig.QUEUE_USER_WITHDRAWN_AUTH)
  public void handleUserWithdrawn(IntegrationEvent integrationEvent) {
    log.info("사용자 탈퇴 이벤트 수신. eventId: {}, traceId: {}",
        integrationEvent.getEventId(), integrationEvent.getTraceId());

    EventContext.run(integrationEvent, event -> {
      UserStatusChangedEvent payload = event.getPayloadAs(UserStatusChangedEvent.class);
      log.info("탈퇴 처리 시작. userId: {}, userType: {}", payload.getUserId(), payload.getUserType());

      authCommandService.handleUserWithdrawn(payload.getUserId());
    });
  }

  /**
   * 사용자 정지 이벤트를 수신하여 처리한다.
   *
   * @param integrationEvent IntegrationEvent
   */
  @RabbitListener(queues = RabbitMQConfig.QUEUE_USER_SUSPENDED_AUTH)
  public void handleUserSuspended(IntegrationEvent integrationEvent) {
    log.info("사용자 정지 이벤트 수신. eventId: {}, traceId: {}",
        integrationEvent.getEventId(), integrationEvent.getTraceId());

    EventContext.run(integrationEvent, event -> {
      UserStatusChangedEvent payload = event.getPayloadAs(UserStatusChangedEvent.class);
      log.info("정지 처리 시작. userId: {}, userType: {}", payload.getUserId(), payload.getUserType());

      authCommandService.handleUserSuspended(payload.getUserId());
    });
  }

  /**
   * 사용자 활성화 이벤트를 수신하여 처리한다.
   *
   * @param integrationEvent IntegrationEvent
   */
  @RabbitListener(queues = RabbitMQConfig.QUEUE_USER_ACTIVATED_AUTH)
  public void handleUserActivated(IntegrationEvent integrationEvent) {
    log.info("사용자 활성화 이벤트 수신. eventId: {}, traceId: {}",
        integrationEvent.getEventId(), integrationEvent.getTraceId());

    EventContext.run(integrationEvent, event -> {
      UserStatusChangedEvent payload = event.getPayloadAs(UserStatusChangedEvent.class);
      log.info("활성화 처리 시작. userId: {}, userType: {}", payload.getUserId(), payload.getUserType());

      authCommandService.handleUserActivated(payload.getUserId());
    });
  }
}