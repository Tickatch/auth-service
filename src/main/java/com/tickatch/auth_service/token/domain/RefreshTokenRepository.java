package com.tickatch.auth_service.token.domain;

import com.tickatch.auth_service.token.domain.repository.dto.RefreshTokenSearchCondition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * RefreshToken 리포지토리 인터페이스.
 *
 * <p>도메인 레이어에서 정의하고, 인프라스트럭처 레이어에서 구현한다. DIP(의존성 역전 원칙)를 적용하여 도메인 로직과 영속성 기술을 분리한다.
 *
 * @author Tickatch
 * @since 1.0.0
 * @see com.tickatch.auth_service.token.domain.repository.RefreshTokenRepositoryImpl
 */
public interface RefreshTokenRepository {

  /**
   * RefreshToken을 저장한다.
   *
   * @param refreshToken 저장할 RefreshToken 엔티티
   * @return 저장된 RefreshToken 엔티티
   */
  RefreshToken save(RefreshToken refreshToken);

  /**
   * ID로 RefreshToken을 조회한다.
   *
   * @param id RefreshToken ID
   * @return 조회된 RefreshToken (없으면 empty)
   */
  Optional<RefreshToken> findById(UUID id);

  /**
   * 토큰 값으로 RefreshToken을 조회한다.
   *
   * <p>토큰 갱신 시 사용한다.
   *
   * @param token 토큰 값
   * @return 조회된 RefreshToken (없으면 empty)
   */
  Optional<RefreshToken> findByToken(String token);

  /**
   * Auth ID로 모든 RefreshToken을 조회한다.
   *
   * <p>사용자의 모든 세션 목록 조회에 사용한다.
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
   * @return 사용 가능한 RefreshToken 목록
   */
  List<RefreshToken> findAllUsableByAuthId(UUID authId);

  /**
   * Auth ID로 모든 RefreshToken을 삭제한다.
   *
   * <p>전체 로그아웃 시 사용한다.
   *
   * @param authId Auth ID
   */
  void deleteAllByAuthId(UUID authId);

  /**
   * Auth ID로 모든 RefreshToken을 폐기한다.
   *
   * <p>토큰 재사용 감지 등 보안 이슈 발생 시 사용한다. 실제 삭제가 아닌 revoked 플래그를 true로 설정한다.
   *
   * @param authId Auth ID
   * @return 폐기된 토큰 수
   */
  int revokeAllByAuthId(UUID authId);

  /**
   * 만료되었거나 폐기된 토큰을 삭제한다.
   *
   * <p>스케줄러에서 정기적으로 호출하여 불필요한 토큰을 정리한다.
   *
   * @return 삭제된 토큰 수
   */
  int deleteExpiredAndRevokedTokens();

  /**
   * 검색 조건에 맞는 RefreshToken 목록을 페이징하여 조회한다.
   *
   * <p>관리자 화면에서 사용한다.
   *
   * @param condition 검색 조건
   * @param pageable 페이징 정보
   * @return 페이징된 RefreshToken 목록
   */
  Page<RefreshToken> findAllByCondition(RefreshTokenSearchCondition condition, Pageable pageable);
}