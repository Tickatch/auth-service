package com.tickatch.auth_service.token.infrastructure.adapter;

import com.tickatch.auth_service.auth.domain.AuthRepository;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import com.tickatch.auth_service.token.application.port.out.AuthPort;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * AuthPort 구현체.
 *
 * <p>Token 도메인에서 Auth 도메인 정보를 조회할 때 사용되는 어댑터이다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
public class AuthAdapter implements AuthPort {

  private final AuthRepository authRepository;

  @Override
  public Optional<UserType> findUserTypeByAuthId(UUID authId) {
    return authRepository.findById(authId).map(auth -> auth.getUserType());
  }
}
