package com.tickatch.auth_service.token.domain.repository;

import com.tickatch.auth_service.token.domain.RefreshToken;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * RefreshToken JPA 리포지토리.
 *
 * <p>Spring Data JPA 기본 CRUD 기능을 제공한다. {@link RefreshTokenRepositoryImpl}에서 내부적으로 사용된다.
 *
 * @author Tickatch
 * @since 1.0.0
 * @see RefreshTokenRepositoryImpl
 */
public interface RefreshTokenJpaRepository extends JpaRepository<RefreshToken, UUID> {

  /**
   * 토큰 값으로 RefreshToken을 조회한다.
   *
   * @param token 토큰 값
   * @return 조회된 RefreshToken (없으면 empty)
   */
  Optional<RefreshToken> findByToken(String token);

  /**
   * Auth ID로 모든 RefreshToken을 조회한다.
   *
   * @param authId Auth ID
   * @return 해당 사용자의 모든 RefreshToken 목록
   */
  List<RefreshToken> findAllByAuthId(UUID authId);

  /**
   * Auth ID로 사용 가능한 RefreshToken을 조회한다.
   *
   * <p>폐기되지 않고 만료되지 않은 토큰만 조회한다.
   *
   * @param authId Auth ID
   * @param now 현재 시간
   * @return 사용 가능한 RefreshToken 목록
   */
  @Query(
      "SELECT rt FROM RefreshToken rt "
          + "WHERE rt.authId = :authId AND rt.revoked = false AND rt.expiresAt > :now")
  List<RefreshToken> findAllUsableByAuthId(
      @Param("authId") UUID authId, @Param("now") LocalDateTime now);

  /**
   * Auth ID로 모든 RefreshToken을 삭제한다.
   *
   * @param authId Auth ID
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM RefreshToken rt WHERE rt.authId = :authId")
  void deleteAllByAuthId(@Param("authId") UUID authId);

  /**
   * Auth ID로 모든 RefreshToken을 폐기한다.
   *
   * @param authId Auth ID
   * @return 폐기된 토큰 수
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "UPDATE RefreshToken rt SET rt.revoked = true "
          + "WHERE rt.authId = :authId AND rt.revoked = false")
  int revokeAllByAuthId(@Param("authId") UUID authId);

  /**
   * 만료되었거나 폐기된 토큰을 삭제한다.
   *
   * @param now 현재 시간
   * @return 삭제된 토큰 수
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true OR rt.expiresAt < :now")
  int deleteExpiredAndRevokedTokens(@Param("now") LocalDateTime now);
}