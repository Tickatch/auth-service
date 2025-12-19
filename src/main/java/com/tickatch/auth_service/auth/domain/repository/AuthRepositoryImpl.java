package com.tickatch.auth_service.auth.domain.repository;

import static com.tickatch.auth_service.auth.domain.QAuth.auth;
import static com.tickatch.auth_service.auth.domain.QAuthProvider.authProvider;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tickatch.auth_service.auth.domain.Auth;
import com.tickatch.auth_service.auth.domain.AuthRepository;
import com.tickatch.auth_service.auth.domain.repository.dto.AuthSearchCondition;
import com.tickatch.auth_service.auth.domain.vo.AuthStatus;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * Auth 리포지토리 구현체.
 *
 * <p>JPA와 QueryDSL을 사용하여 Auth 데이터를 조회/저장한다. 동적 쿼리를 통해 다양한 검색 조건을 지원한다.
 *
 * @author Tickatch
 * @since 1.0.0
 * @see AuthRepository
 */
@Repository
@RequiredArgsConstructor
public class AuthRepositoryImpl implements AuthRepository {

  private final AuthJpaRepository authJpaRepository;
  private final JPAQueryFactory queryFactory;

  @Override
  public Auth save(Auth auth) {
    return authJpaRepository.save(auth);
  }

  @Override
  public Optional<Auth> findById(UUID id) {
    return authJpaRepository.findById(id);
  }

  @Override
  public Optional<Auth> findByEmailAndUserType(String email, UserType userType) {
    return authJpaRepository.findByEmailAndUserType(email, userType);
  }

  @Override
  public boolean existsByEmailAndUserType(String email, UserType userType) {
    return authJpaRepository.existsByEmailAndUserType(email, userType);
  }

  @Override
  public Optional<Auth> findByProviderAndProviderUserId(
      ProviderType provider, String providerUserId) {
    return authJpaRepository.findByProviderAndProviderUserId(provider, providerUserId);
  }

  /**
   * {@inheritDoc}
   *
   * <p>QueryDSL을 사용하여 동적 검색 조건을 적용한다. Provider 정보를 fetch join하여 N+1 문제를 방지한다.
   */
  @Override
  public Page<Auth> findAllByCondition(AuthSearchCondition condition, Pageable pageable) {
    List<Auth> content =
        queryFactory
            .selectFrom(auth)
            .distinct()
            .leftJoin(auth.providers, authProvider)
            .fetchJoin()
            .where(
                emailContains(condition.getEmail()),
                userTypeEq(condition.getUserType()),
                statusEq(condition.getStatus()),
                hasProviderType(condition.getProviderType()))
            .orderBy(getOrderSpecifiers(pageable.getSort()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    JPAQuery<Long> countQuery =
        queryFactory
            .select(auth.count())
            .from(auth)
            .where(
                emailContains(condition.getEmail()),
                userTypeEq(condition.getUserType()),
                statusEq(condition.getStatus()),
                hasProviderType(condition.getProviderType()));

    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
  }

  /**
   * 이메일 부분 일치 검색 조건.
   *
   * @param email 검색할 이메일
   * @return 이메일 포함 조건 (null이면 조건 미적용)
   */
  private BooleanExpression emailContains(String email) {
    return StringUtils.hasText(email) ? auth.email.containsIgnoreCase(email) : null;
  }

  /**
   * 사용자 유형 일치 검색 조건.
   *
   * @param userType 검색할 사용자 유형
   * @return 사용자 유형 일치 조건 (null이면 조건 미적용)
   */
  private BooleanExpression userTypeEq(UserType userType) {
    return userType != null ? auth.userType.eq(userType) : null;
  }

  /**
   * 계정 상태 일치 검색 조건.
   *
   * @param status 검색할 계정 상태
   * @return 계정 상태 일치 조건 (null이면 조건 미적용)
   */
  private BooleanExpression statusEq(AuthStatus status) {
    return status != null ? auth.status.eq(status) : null;
  }

  /**
   * 특정 소셜 로그인 제공자 연동 여부 검색 조건.
   *
   * @param providerType 검색할 소셜 로그인 제공자
   * @return 해당 제공자 연동 조건 (null이면 조건 미적용)
   */
  private BooleanExpression hasProviderType(ProviderType providerType) {
    if (providerType == null) {
      return null;
    }
    return auth.providers.any().provider.eq(providerType);
  }

  /**
   * 정렬 조건을 OrderSpecifier 배열로 변환한다.
   *
   * @param sort Spring Data Sort 객체
   * @return QueryDSL OrderSpecifier 배열
   */
  private OrderSpecifier<?>[] getOrderSpecifiers(Sort sort) {
    List<OrderSpecifier<?>> orderSpecifiers = new ArrayList<>();

    sort.forEach(
        order -> {
          Order direction = order.isAscending() ? Order.ASC : Order.DESC;
          String property = order.getProperty();

          OrderSpecifier<?> orderSpecifier =
              switch (property) {
                case "email" -> new OrderSpecifier<>(direction, auth.email);
                case "userType" -> new OrderSpecifier<>(direction, auth.userType);
                case "status" -> new OrderSpecifier<>(direction, auth.status);
                case "lastLoginAt" -> new OrderSpecifier<>(direction, auth.lastLoginAt);
                case "createdAt" -> new OrderSpecifier<>(direction, auth.createdAt);
                case "updatedAt" -> new OrderSpecifier<>(direction, auth.updatedAt);
                default -> new OrderSpecifier<>(direction, auth.createdAt);
              };
          orderSpecifiers.add(orderSpecifier);
        });

    if (orderSpecifiers.isEmpty()) {
      orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, auth.createdAt));
    }

    return orderSpecifiers.toArray(new OrderSpecifier[0]);
  }
}
