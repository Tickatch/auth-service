package com.tickatch.auth_service.token.application.service.query;

import com.tickatch.auth_service.token.application.service.query.dto.TokenInfo;
import com.tickatch.auth_service.token.domain.RefreshToken;
import com.tickatch.auth_service.token.domain.RefreshTokenRepository;
import com.tickatch.auth_service.token.domain.repository.dto.RefreshTokenResponse;
import com.tickatch.auth_service.token.domain.repository.dto.RefreshTokenSearchCondition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 토큰 관련 Query 서비스.
 *
 * <p>토큰 조회를 담당한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TokenQueryService {

  private final RefreshTokenRepository refreshTokenRepository;

  /**
   * 토큰 ID로 조회한다.
   *
   * @param tokenId 토큰 ID
   * @return 토큰 정보
   */
  public Optional<TokenInfo> findById(UUID tokenId) {
    return refreshTokenRepository.findById(tokenId)
        .map(TokenInfo::from);
  }

  /**
   * Refresh Token 값으로 조회한다.
   *
   * @param tokenValue Refresh Token 값
   * @return 토큰 정보
   */
  public Optional<TokenInfo> findByToken(String tokenValue) {
    return refreshTokenRepository.findByToken(tokenValue)
        .map(TokenInfo::from);
  }

  /**
   * Auth ID로 모든 토큰을 조회한다.
   *
   * @param authId Auth ID
   * @return 토큰 목록
   */
  public List<TokenInfo> findAllByAuthId(UUID authId) {
    return refreshTokenRepository.findAllByAuthId(authId).stream()
        .map(TokenInfo::from)
        .toList();
  }

  /**
   * Auth ID로 사용 가능한 토큰만 조회한다.
   *
   * @param authId Auth ID
   * @return 사용 가능한 토큰 목록
   */
  public List<TokenInfo> findAllUsableByAuthId(UUID authId) {
    return refreshTokenRepository.findAllUsableByAuthId(authId).stream()
        .map(TokenInfo::from)
        .toList();
  }

  /**
   * 조건으로 토큰을 검색한다 (관리자용).
   *
   * @param condition 검색 조건
   * @param pageable 페이징 정보
   * @return 토큰 목록 (페이징)
   */
  public Page<RefreshTokenResponse> searchTokens(RefreshTokenSearchCondition condition, Pageable pageable) {
    return refreshTokenRepository.findAllByCondition(condition, pageable)
        .map(RefreshTokenResponse::from);
  }

  /**
   * 사용자의 활성 세션 수를 조회한다.
   *
   * @param authId Auth ID
   * @return 활성 세션 수
   */
  public long countActiveSessionsByAuthId(UUID authId) {
    return refreshTokenRepository.findAllUsableByAuthId(authId).size();
  }
}