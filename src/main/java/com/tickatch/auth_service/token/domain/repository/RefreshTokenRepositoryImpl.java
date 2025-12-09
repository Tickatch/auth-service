package com.tickatch.auth_service.token.domain.repository;

import static com.tickatch.auth_service.token.domain.QRefreshToken.refreshToken;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tickatch.auth_service.token.domain.RefreshToken;
import com.tickatch.auth_service.token.domain.RefreshTokenRepository;
import com.tickatch.auth_service.token.domain.repository.dto.RefreshTokenSearchCondition;
import java.time.LocalDateTime;
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
 * RefreshToken 리포지토리 구현체.
 *
 * <p>JPA와 QueryDSL을 사용하여 RefreshToken 데이터를 조회/저장한다. 동적 쿼리를 통해 다양한 검색 조건을 지원한다.
 *
 * @author Tickatch
 * @since 1.0.0
 * @see RefreshTokenRepository
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

  private final RefreshTokenJpaRepository refreshTokenJpaRepository;
  private final JPAQueryFactory queryFactory;

  @Override
  public RefreshToken save(RefreshToken refreshToken) {
    return refreshTokenJpaRepository.save(refreshToken);
  }

  @Override
  public Optional<RefreshToken> findById(UUID id) {
    return refreshTokenJpaRepository.findById(id);
  }

  @Override
  public Optional<RefreshToken> findByToken(String token) {
    return refreshTokenJpaRepository.findByToken(token);
  }

  @Override
  public List<RefreshToken> findAllByAuthId(UUID authId) {
    return refreshTokenJpaRepository.findAllByAuthId(authId);
  }

  @Override
  public List<RefreshToken> findAllUsableByAuthId(UUID authId) {
    return refreshTokenJpaRepository.findAllUsableByAuthId(authId, LocalDateTime.now());
  }

  @Override
  public void deleteAllByAuthId(UUID authId) {
    refreshTokenJpaRepository.deleteAllByAuthId(authId);
  }

  @Override
  public int revokeAllByAuthId(UUID authId) {
    return refreshTokenJpaRepository.revokeAllByAuthId(authId);
  }

  @Override
  public int deleteExpiredAndRevokedTokens() {
    return refreshTokenJpaRepository.deleteExpiredAndRevokedTokens(LocalDateTime.now());
  }

  /**
   * {@inheritDoc}
   *
   * <p>QueryDSL을 사용하여 동적 검색 조건을 적용한다.
   */
  @Override
  public Page<RefreshToken> findAllByCondition(
      RefreshTokenSearchCondition condition, Pageable pageable) {
    LocalDateTime now = LocalDateTime.now();

    List<RefreshToken> content =
        queryFactory
            .selectFrom(refreshToken)
            .where(
                authIdEq(condition.getAuthId()),
                revokedEq(condition.getRevoked()),
                expiredEq(condition.getExpired(), now),
                rememberMeEq(condition.getRememberMe()),
                deviceInfoContains(condition.getDeviceInfo()))
            .orderBy(getOrderSpecifiers(pageable.getSort()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

    JPAQuery<Long> countQuery =
        queryFactory
            .select(refreshToken.count())
            .from(refreshToken)
            .where(
                authIdEq(condition.getAuthId()),
                revokedEq(condition.getRevoked()),
                expiredEq(condition.getExpired(), now),
                rememberMeEq(condition.getRememberMe()),
                deviceInfoContains(condition.getDeviceInfo()));

    return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
  }

  /**
   * Auth ID 일치 검색 조건.
   *
   * @param authId 검색할 Auth ID
   * @return Auth ID 일치 조건 (null이면 조건 미적용)
   */
  private BooleanExpression authIdEq(UUID authId) {
    return authId != null ? refreshToken.authId.eq(authId) : null;
  }

  /**
   * 폐기 여부 검색 조건.
   *
   * @param revoked 폐기 여부
   * @return 폐기 여부 일치 조건 (null이면 조건 미적용)
   */
  private BooleanExpression revokedEq(Boolean revoked) {
    return revoked != null ? refreshToken.revoked.eq(revoked) : null;
  }

  /**
   * 만료 여부 검색 조건.
   *
   * @param expired 만료 여부
   * @param now 현재 시간
   * @return 만료 여부 조건 (null이면 조건 미적용)
   */
  private BooleanExpression expiredEq(Boolean expired, LocalDateTime now) {
    if (expired == null) {
      return null;
    }
    return expired ? refreshToken.expiresAt.before(now) : refreshToken.expiresAt.after(now);
  }

  /**
   * 로그인 유지 여부 검색 조건.
   *
   * @param rememberMe 로그인 유지 여부
   * @return 로그인 유지 여부 일치 조건 (null이면 조건 미적용)
   */
  private BooleanExpression rememberMeEq(Boolean rememberMe) {
    return rememberMe != null ? refreshToken.rememberMe.eq(rememberMe) : null;
  }

  /**
   * 디바이스 정보 부분 일치 검색 조건.
   *
   * @param deviceInfo 검색할 디바이스 정보
   * @return 디바이스 정보 포함 조건 (null이면 조건 미적용)
   */
  private BooleanExpression deviceInfoContains(String deviceInfo) {
    return StringUtils.hasText(deviceInfo)
        ? refreshToken.deviceInfo.containsIgnoreCase(deviceInfo)
        : null;
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
                case "authId" -> new OrderSpecifier<>(direction, refreshToken.authId);
                case "expiresAt" -> new OrderSpecifier<>(direction, refreshToken.expiresAt);
                case "createdAt" -> new OrderSpecifier<>(direction, refreshToken.createdAt);
                case "revoked" -> new OrderSpecifier<>(direction, refreshToken.revoked);
                case "rememberMe" -> new OrderSpecifier<>(direction, refreshToken.rememberMe);
                case "deviceInfo" -> new OrderSpecifier<>(direction, refreshToken.deviceInfo);
                default -> new OrderSpecifier<>(direction, refreshToken.createdAt);
              };
          orderSpecifiers.add(orderSpecifier);
        });

    if (orderSpecifiers.isEmpty()) {
      orderSpecifiers.add(new OrderSpecifier<>(Order.DESC, refreshToken.createdAt));
    }

    return orderSpecifiers.toArray(new OrderSpecifier[0]);
  }
}