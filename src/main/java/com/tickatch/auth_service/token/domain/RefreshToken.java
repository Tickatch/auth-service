package com.tickatch.auth_service.token.domain;

import com.tickatch.auth_service.token.domain.exception.TokenErrorCode;
import com.tickatch.auth_service.token.domain.exception.TokenException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리프레시 토큰 Aggregate Root.
 *
 * <p>JWT 리프레시 토큰을 관리하며, 토큰 Rotation 방식을 지원한다.
 *
 * <p>비즈니스 규칙:
 *
 * <ul>
 *   <li>리프레시 토큰은 1회 사용 후 새 토큰으로 교체된다 (Rotation)
 *   <li>기본 만료 기간은 14일이다
 *   <li>만료된 토큰으로 갱신 시도 시 예외가 발생한다
 *   <li>이미 사용된 토큰 재사용 감지 시 모든 토큰 폐기 (보안)
 * </ul>
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

  /* 기본 만료 기간 (시간) - 로그인 유지 미선택 시 */
  private static final long DEFAULT_EXPIRY_HOURS = 1;
  /* 로그인 유지 만료 기간 (일) - 로그인 유지 선택 시 */
  private static final long REMEMBER_ME_EXPIRY_DAYS = 30;

  /* 토큰 ID */
  @Id
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;

  /* 인증 ID */
  @Column(name = "auth_id", nullable = false)
  private UUID authId;

  /* 토큰 값 */
  @Column(name = "token", nullable = false, unique = true, length = 500)
  private String token;

  /* 디바이스 정보 */
  @Column(name = "device_info")
  private String deviceInfo;

  /* 만료 일시 */
  @Column(name = "expires_at", nullable = false)
  private LocalDateTime expiresAt;

  /* 생성 일시 */
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  /* 폐기 여부 */
  @Column(name = "revoked", nullable = false)
  private boolean revoked;

  /* 로그인 유지 여부 */
  @Column(name = "remember_me", nullable = false)
  private boolean rememberMe;

  private RefreshToken(
      UUID authId, String token, String deviceInfo, LocalDateTime expiresAt, boolean rememberMe) {
    this.id = UUID.randomUUID();
    this.authId = authId;
    this.token = token;
    this.deviceInfo = deviceInfo;
    this.expiresAt = expiresAt;
    this.createdAt = LocalDateTime.now();
    this.revoked = false;
    this.rememberMe = rememberMe;
  }

  /**
   * 새 리프레시 토큰을 생성한다.
   *
   * <p>로그인 유지 여부에 따라 만료 기간이 달라진다.
   *
   * <ul>
   *   <li>로그인 유지 미선택: 1시간
   *   <li>로그인 유지 선택: 30일
   * </ul>
   *
   * @param authId Auth ID
   * @param token 토큰 값
   * @param deviceInfo 디바이스 정보
   * @param rememberMe 로그인 유지 여부
   * @return 생성된 RefreshToken
   */
  public static RefreshToken create(
      UUID authId, String token, String deviceInfo, boolean rememberMe) {
    LocalDateTime expiresAt =
        rememberMe
            ? LocalDateTime.now().plusDays(REMEMBER_ME_EXPIRY_DAYS)
            : LocalDateTime.now().plusHours(DEFAULT_EXPIRY_HOURS);
    return new RefreshToken(authId, token, deviceInfo, expiresAt, rememberMe);
  }

  /**
   * 만료 기간을 지정하여 리프레시 토큰을 생성한다.
   *
   * <p>테스트 또는 특수한 경우에 사용한다.
   *
   * @param authId Auth ID
   * @param token 토큰 값
   * @param deviceInfo 디바이스 정보
   * @param expiresAt 만료 일시
   * @param rememberMe 로그인 유지 여부
   * @return 생성된 RefreshToken
   */
  public static RefreshToken createWithExpiry(
      UUID authId, String token, String deviceInfo, LocalDateTime expiresAt, boolean rememberMe) {
    return new RefreshToken(authId, token, deviceInfo, expiresAt, rememberMe);
  }

  /**
   * 토큰을 새 값으로 교체한다 (Rotation).
   *
   * <p>만료 기간도 함께 갱신된다. 로그인 유지 여부에 따라 만료 기간이 달라진다.
   *
   * @param newToken 새 토큰 값
   * @throws TokenException 토큰이 만료되었거나 이미 폐기된 경우
   */
  public void rotate(String newToken) {
    validateUsable();
    this.token = newToken;
    this.expiresAt =
        this.rememberMe
            ? LocalDateTime.now().plusDays(REMEMBER_ME_EXPIRY_DAYS)
            : LocalDateTime.now().plusHours(DEFAULT_EXPIRY_HOURS);
  }

  /** 토큰을 폐기한다. */
  public void revoke() {
    this.revoked = true;
  }

  /**
   * 토큰이 만료되었는지 확인한다.
   *
   * @return 만료되었으면 true
   */
  public boolean isExpired() {
    return LocalDateTime.now().isAfter(this.expiresAt);
  }

  /**
   * 토큰이 사용 가능한 상태인지 확인한다.
   *
   * @return 폐기되지 않고 만료되지 않았으면 true
   */
  public boolean isUsable() {
    return !revoked && !isExpired();
  }

  /**
   * 토큰이 사용 가능한 상태인지 검증한다.
   *
   * @throws TokenException 토큰이 폐기되었거나 만료된 경우
   */
  public void validateUsable() {
    if (this.revoked) {
      throw new TokenException(TokenErrorCode.TOKEN_ALREADY_REVOKED);
    }
    if (isExpired()) {
      throw new TokenException(TokenErrorCode.REFRESH_TOKEN_EXPIRED);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RefreshToken that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
