package com.tickatch.auth_service.global.config;

import io.github.tickatch.common.security.AuthenticatedUser;
import io.github.tickatch.common.security.UserType;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class ActorExtractor {

  private ActorExtractor() {}

  public static ActorInfo extract() {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {

      String actorType =
          user.getUserType() != null
              ? user.getUserType().name() // ADMIN / SELLER / CUSTOMER
              : "USER_UNKNOWN";

      return new ActorInfo(actorType, UUID.fromString(user.getUserId()));
    }
    return new ActorInfo("SYSTEM", null);
  }

  public static ActorInfo extract(String authId, UserType userType) {

      return new ActorInfo(userType.toString(), UUID.fromString(authId));
  }

  public record ActorInfo(String actorType, UUID actorUserId) {}
}
