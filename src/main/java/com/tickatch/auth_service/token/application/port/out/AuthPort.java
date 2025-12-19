package com.tickatch.auth_service.token.application.port.out;

import com.tickatch.auth_service.auth.domain.vo.UserType;
import java.util.Optional;
import java.util.UUID;

/**
 * Auth 정보 조회를 위한 아웃바운드 포트.
 *
 * <p>Token 도메인에서 Auth 도메인의 정보가 필요할 때 이 인터페이스를 통해 접근한다. 도메인 간 직접 의존을 피하고 느슨한 결합을 유지한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
public interface AuthPort {

  /**
   * Auth ID로 사용자 유형을 조회한다.
   *
   * <p>토큰 갱신 시 Access Token 생성에 필요한 userType을 조회한다.
   *
   * @param authId Auth ID
   * @return 사용자 유형 (없으면 empty)
   */
  Optional<UserType> findUserTypeByAuthId(UUID authId);
}
