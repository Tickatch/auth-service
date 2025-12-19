package com.tickatch.auth_service.auth.infrastructure.messaging.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.tickatch.common.event.DomainEvent;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

/**
 * 사용자 상태 변경 이벤트 (수신용).
 *
 * <p>User Service에서 발행하는 사용자 상태 변경 이벤트를 역직렬화하기 위한 클래스.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Getter
public class UserStatusChangedEvent extends DomainEvent {

  private static final String AGGREGATE_TYPE = "User";

  /** 사용자 ID (= Auth ID) */
  private final UUID userId;

  /** 사용자 유형 (CUSTOMER, SELLER, ADMIN) */
  private final String userType;

  /** 상태 변경 유형 (WITHDRAWN, SUSPENDED, ACTIVATED) */
  private final String statusChangeType;

  /** 라우팅 키 */
  private final String routingKey;

  /** JSON 역직렬화용 생성자. */
  @JsonCreator
  public UserStatusChangedEvent(
      @JsonProperty("eventId") String eventId,
      @JsonProperty("occurredAt") Instant occurredAt,
      @JsonProperty("version") int version,
      @JsonProperty("userId") UUID userId,
      @JsonProperty("userType") String userType,
      @JsonProperty("statusChangeType") String statusChangeType,
      @JsonProperty("routingKey") String routingKey) {
    super(eventId, occurredAt, version);
    this.userId = userId;
    this.userType = userType;
    this.statusChangeType = statusChangeType;
    this.routingKey = routingKey;
  }

  @Override
  public String getAggregateId() {
    return userId.toString();
  }

  @Override
  public String getAggregateType() {
    return AGGREGATE_TYPE;
  }

  @Override
  public String getRoutingKey() {
    return routingKey;
  }

  /** 탈퇴 이벤트인지 확인한다. */
  public boolean isWithdrawn() {
    return "WITHDRAWN".equals(statusChangeType);
  }

  /** 정지 이벤트인지 확인한다. */
  public boolean isSuspended() {
    return "SUSPENDED".equals(statusChangeType);
  }

  /** 활성화 이벤트인지 확인한다. */
  public boolean isActivated() {
    return "ACTIVATED".equals(statusChangeType);
  }
}
