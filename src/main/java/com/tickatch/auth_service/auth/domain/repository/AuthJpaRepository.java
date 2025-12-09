package com.tickatch.auth_service.auth.domain.repository;

import com.tickatch.auth_service.auth.domain.Auth;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Auth JPA 리포지토리.
 *
 * <p>Spring Data JPA 기본 CRUD 기능을 제공한다. {@link AuthRepositoryImpl}에서 내부적으로 사용된다.
 *
 * @author Tickatch
 * @since 1.0.0
 * @see AuthRepositoryImpl
 */
public interface AuthJpaRepository extends JpaRepository<Auth, UUID> {

  /**
   * 이메일과 사용자 유형으로 Auth를 조회한다.
   *
   * @param email 이메일
   * @param userType 사용자 유형
   * @return 조회된 Auth (없으면 empty)
   */
  Optional<Auth> findByEmailAndUserType(String email, UserType userType);

  /**
   * 이메일과 사용자 유형으로 Auth 존재 여부를 확인한다.
   *
   * @param email 이메일
   * @param userType 사용자 유형
   * @return 존재하면 true
   */
  boolean existsByEmailAndUserType(String email, UserType userType);

  /**
   * 소셜 로그인 제공자 정보로 Auth를 조회한다.
   *
   * @param provider 소셜 로그인 제공자
   * @param providerUserId 제공자 측 사용자 ID
   * @return 조회된 Auth (없으면 empty)
   */
  @Query(
      "SELECT a FROM Auth a JOIN a.providers p "
          + "WHERE p.provider = :provider AND p.providerUserId = :providerUserId")
  Optional<Auth> findByProviderAndProviderUserId(
      @Param("provider") ProviderType provider, @Param("providerUserId") String providerUserId);
}