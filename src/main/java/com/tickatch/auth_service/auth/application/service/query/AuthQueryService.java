package com.tickatch.auth_service.auth.application.service.query;

import com.tickatch.auth_service.auth.application.service.query.dto.AuthInfo;
import com.tickatch.auth_service.auth.domain.AuthRepository;
import com.tickatch.auth_service.auth.domain.exception.AuthErrorCode;
import com.tickatch.auth_service.auth.domain.exception.AuthException;
import com.tickatch.auth_service.auth.domain.repository.dto.AuthResponse;
import com.tickatch.auth_service.auth.domain.repository.dto.AuthSearchCondition;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 정보 Query 서비스.
 *
 * <p>인증 정보 조회를 담당한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthQueryService {

  private final AuthRepository authRepository;

  /**
   * Auth ID로 조회한다.
   *
   * @param authId Auth ID
   * @return Auth 정보
   * @throws AuthException 계정을 찾을 수 없는 경우
   */
  public AuthInfo findById(UUID authId) {
    return authRepository
        .findById(authId)
        .map(AuthInfo::from)
        .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_NOT_FOUND));
  }

  /**
   * Auth ID로 조회한다 (Optional).
   *
   * @param authId Auth ID
   * @return Auth 정보
   */
  public Optional<AuthInfo> findByIdOptional(UUID authId) {
    return authRepository.findById(authId).map(AuthInfo::from);
  }

  /**
   * 이메일과 사용자 유형으로 조회한다.
   *
   * @param email 이메일
   * @param userType 사용자 유형
   * @return Auth 정보
   */
  public Optional<AuthInfo> findByEmailAndUserType(String email, UserType userType) {
    return authRepository.findByEmailAndUserType(email, userType).map(AuthInfo::from);
  }

  /**
   * 소셜 로그인 정보로 조회한다.
   *
   * @param providerType 소셜 로그인 제공자
   * @param providerUserId 제공자 측 사용자 ID
   * @return Auth 정보
   */
  public Optional<AuthInfo> findByProviderInfo(ProviderType providerType, String providerUserId) {
    return authRepository
        .findByProviderAndProviderUserId(providerType, providerUserId)
        .map(AuthInfo::from);
  }

  /**
   * 이메일 중복 여부를 확인한다.
   *
   * @param email 이메일
   * @param userType 사용자 유형
   * @return 중복이면 true
   */
  public boolean existsByEmailAndUserType(String email, UserType userType) {
    return authRepository.existsByEmailAndUserType(email, userType);
  }

  /**
   * 조건으로 Auth를 검색한다 (관리자용).
   *
   * @param condition 검색 조건
   * @param pageable 페이징 정보
   * @return Auth 목록 (페이징)
   */
  public Page<AuthResponse> searchAuths(AuthSearchCondition condition, Pageable pageable) {
    return authRepository.findAllByCondition(condition, pageable).map(AuthResponse::from);
  }
}
