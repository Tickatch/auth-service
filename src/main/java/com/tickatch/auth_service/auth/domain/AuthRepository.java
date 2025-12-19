package com.tickatch.auth_service.auth.domain;

import com.tickatch.auth_service.auth.domain.repository.dto.AuthSearchCondition;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Auth 리포지토리 인터페이스.
 *
 * <p>도메인 레이어에서 정의하고, 인프라스트럭처 레이어에서 구현한다. DIP(의존성 역전 원칙)를 적용하여 도메인 로직과 영속성 기술을 분리한다.
 *
 * @author Tickatch
 * @since 1.0.0
 * @see com.tickatch.auth_service.auth.domain.repository.AuthRepositoryImpl
 */
public interface AuthRepository {

  /**
   * Auth를 저장한다.
   *
   * @param auth 저장할 Auth 엔티티
   * @return 저장된 Auth 엔티티
   */
  Auth save(Auth auth);

  /**
   * ID로 Auth를 조회한다.
   *
   * @param id Auth ID
   * @return 조회된 Auth (없으면 empty)
   */
  Optional<Auth> findById(UUID id);

  /**
   * 이메일과 사용자 유형으로 Auth를 조회한다.
   *
   * <p>로그인 시 사용한다.
   *
   * @param email 이메일
   * @param userType 사용자 유형
   * @return 조회된 Auth (없으면 empty)
   */
  Optional<Auth> findByEmailAndUserType(String email, UserType userType);

  /**
   * 이메일과 사용자 유형으로 Auth 존재 여부를 확인한다.
   *
   * <p>회원가입 시 중복 체크에 사용한다.
   *
   * @param email 이메일
   * @param userType 사용자 유형
   * @return 존재하면 true
   */
  boolean existsByEmailAndUserType(String email, UserType userType);

  /**
   * 소셜 로그인 제공자 정보로 Auth를 조회한다.
   *
   * <p>소셜 로그인 시 사용한다.
   *
   * @param provider 소셜 로그인 제공자
   * @param providerUserId 제공자 측 사용자 ID
   * @return 조회된 Auth (없으면 empty)
   */
  Optional<Auth> findByProviderAndProviderUserId(ProviderType provider, String providerUserId);

  /**
   * 검색 조건에 맞는 Auth 목록을 페이징하여 조회한다.
   *
   * <p>관리자 화면에서 사용한다. Provider 정보를 함께 fetch join하여 조회한다.
   *
   * @param condition 검색 조건
   * @param pageable 페이징 정보
   * @return 페이징된 Auth 목록
   */
  Page<Auth> findAllByCondition(AuthSearchCondition condition, Pageable pageable);
}
