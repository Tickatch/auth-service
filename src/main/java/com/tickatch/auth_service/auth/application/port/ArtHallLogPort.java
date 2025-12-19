package com.tickatch.auth_service.auth.application.port;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ArtHallLogPort {

  void publishAction(
      Long artHallId,
      String actionType,
      String actorType,
      UUID actorUserId,
      LocalDateTime occurredAt);
}
