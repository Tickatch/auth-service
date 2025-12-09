package com.tickatch.auth_service.auth.domain;

import com.tickatch.auth_service.auth.domain.exception.AuthErrorCode;
import com.tickatch.auth_service.auth.domain.exception.AuthException;
import com.tickatch.auth_service.auth.domain.vo.AuthStatus;
import com.tickatch.auth_service.auth.domain.vo.Password;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import com.tickatch.auth_service.global.domain.AbstractAuditEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 인증 Aggregate Root.
 *
 * <p>사용자 인증 정보를 관리하며, 로그인/로그아웃, 비밀번호, 소셜 로그인 연동을 담당한다.
 *
 * <p>비즈니스 규칙:
 *
 * <ul>
 *   <li>email + userType 조합이 유일해야 한다 (동일 이메일로 구매자/판매자 분리 가입 가능)
 *   <li>로그인 실패 5회 초과 시 계정이 잠금된다
 *   <li>소셜 로그인으로 가입해도 비밀번호는 필수이다
 * </ul>
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Entity
@Table(
    name = "auths",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_auth_email_user_type", columnNames = {"email", "user_type"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auth extends AbstractAuditEntity {

  /* 최대 로그인 실패 허용 횟수 */
  private static final int MAX_LOGIN_FAIL_COUNT = 5;

  /* 인증 ID */
  @Id
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;

  /* 이메일 */
  @Column(name = "email", nullable = false)
  private String email;

  /* 사용자 유형 */
  @Enumerated(EnumType.STRING)
  @Column(name = "user_type", nullable = false, length = 20)
  private UserType userType;

  /* 비밀번호 */
  @Embedded
  private Password password;

  /* 계정 상태 */
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private AuthStatus status;

  /* 로그인 실패 횟수 */
  @Column(name = "login_fail_count", nullable = false)
  private int loginFailCount;

  /* 마지막 로그인 일시 */
  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  /* 소셜 로그인 연동 목록 */
  @OneToMany(mappedBy = "auth", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<AuthProvider> providers = new ArrayList<>();

  // ========================================
  // 생성자 및 팩토리 메서드
  // ========================================

  private Auth(UUID id, String email, UserType userType, Password password, String createdBy) {
    validateEmail(email);
    validateUserType(userType);

    this.id = id;
    this.email = email;
    this.userType = userType;
    this.password = password;
    this.status = AuthStatus.ACTIVE;
    this.loginFailCount = 0;
    this.createBy(createdBy);
    this.updateBy(createdBy);
  }

  /**
   * 새로운 Auth를 생성한다 (회원가입).
   *
   * @param email 이메일
   * @param rawPassword 원본 비밀번호
   * @param userType 사용자 유형
   * @param encoder 비밀번호 인코더
   * @param createdBy 생성자
   * @return 생성된 Auth
   */
  public static Auth register(
      String email,
      String rawPassword,
      UserType userType,
      PasswordEncoder encoder,
      String createdBy) {
    Password password = Password.create(rawPassword, encoder);
    return new Auth(UUID.randomUUID(), email, userType, password, createdBy);
  }

  /**
   * 소셜 로그인으로 Auth를 생성한다.
   *
   * <p>소셜 로그인은 CUSTOMER 유형만 허용된다. 비밀번호는 필수이며, 소셜 서비스 장애 시에도 일반 로그인이 가능하도록 한다.
   *
   * @param email 이메일
   * @param rawPassword 원본 비밀번호
   * @param userType 사용자 유형 (CUSTOMER만 허용)
   * @param providerType 소셜 로그인 제공자
   * @param providerUserId 제공자 측 사용자 ID
   * @param encoder 비밀번호 인코더
   * @param createdBy 생성자
   * @return 생성된 Auth
   * @throws AuthException CUSTOMER가 아닌 경우
   */
  public static Auth registerWithOAuth(
      String email,
      String rawPassword,
      UserType userType,
      ProviderType providerType,
      String providerUserId,
      PasswordEncoder encoder,
      String createdBy) {
    validateOAuthAllowedForUserType(userType);

    Password password = Password.create(rawPassword, encoder);
    Auth auth = new Auth(UUID.randomUUID(), email, userType, password, createdBy);
    auth.addProviderInternal(providerType, providerUserId);
    return auth;
  }

  // ========================================
  // 로그인 관련
  // ========================================

  /**
   * 로그인 성공을 기록한다.
   *
   * <p>로그인 실패 횟수를 초기화하고 마지막 로그인 시간을 갱신한다.
   *
   * @throws AuthException 계정이 잠금 또는 탈퇴 상태인 경우
   */
  public void recordLoginSuccess() {
    validateCanLogin();

    this.loginFailCount = 0;
    this.lastLoginAt = LocalDateTime.now();
  }

  /**
   * 로그인 실패를 기록한다.
   *
   * <p>실패 횟수가 최대치를 초과하면 계정이 잠금된다.
   *
   * @throws AuthException 계정이 탈퇴 상태인 경우
   */
  public void recordLoginFailure() {
    validateNotWithdrawn();

    this.loginFailCount++;
    if (this.loginFailCount >= MAX_LOGIN_FAIL_COUNT) {
      this.status = AuthStatus.LOCKED;
    }
  }

  /**
   * 비밀번호 일치 여부를 확인한다.
   *
   * @param rawPassword 원본 비밀번호
   * @param encoder 비밀번호 인코더
   * @return 일치하면 true
   */
  public boolean matchesPassword(String rawPassword, PasswordEncoder encoder) {
    if (this.password == null) {
      return false;
    }
    return this.password.matches(rawPassword, encoder);
  }

  // ========================================
  // 비밀번호 관련
  // ========================================

  /**
   * 비밀번호를 변경한다.
   *
   * @param rawPassword 새 원본 비밀번호
   * @param encoder 비밀번호 인코더
   * @param updatedBy 수정자
   * @throws AuthException 계정이 잠금 또는 탈퇴 상태인 경우
   */
  public void changePassword(String rawPassword, PasswordEncoder encoder, String updatedBy) {
    validateCanLogin();

    this.password = Password.create(rawPassword, encoder);
    this.updateBy(updatedBy);
  }

  /**
   * 비밀번호를 초기화한다.
   *
   * <p>계정 잠금을 해제하고 로그인 실패 횟수를 초기화한다.
   *
   * @param rawPassword 새 원본 비밀번호
   * @param encoder 비밀번호 인코더
   * @param updatedBy 수정자
   * @throws AuthException 계정이 탈퇴 상태인 경우
   */
  public void resetPassword(String rawPassword, PasswordEncoder encoder, String updatedBy) {
    validateNotWithdrawn();

    this.password = Password.create(rawPassword, encoder);
    this.status = AuthStatus.ACTIVE;
    this.loginFailCount = 0;
    this.updateBy(updatedBy);
  }

  /**
   * 비밀번호 존재 여부를 확인한다.
   *
   * @return 비밀번호가 설정되어 있으면 true
   */
  public boolean hasPassword() {
    return this.password != null && this.password.hasValue();
  }

  // ========================================
  // 상태 변경
  // ========================================

  /**
   * 계정을 잠금한다.
   *
   * @throws AuthException 계정이 탈퇴 상태인 경우
   */
  public void lock() {
    validateNotWithdrawn();
    this.status = AuthStatus.LOCKED;
  }

  /**
   * 계정 잠금을 해제한다.
   *
   * <p>로그인 실패 횟수도 함께 초기화한다.
   */
  public void unlock() {
    if (!this.status.isLocked()) {
      return;
    }
    this.status = AuthStatus.ACTIVE;
    this.loginFailCount = 0;
  }

  /**
   * 회원을 탈퇴 처리한다.
   *
   * @param deletedBy 삭제자
   * @throws AuthException 이미 탈퇴한 계정인 경우
   */
  public void withdraw(String deletedBy) {
    if (this.status.isWithdrawn()) {
      throw new AuthException(AuthErrorCode.ALREADY_WITHDRAWN);
    }

    this.status = AuthStatus.WITHDRAWN;
    this.delete(deletedBy);
  }

  // ========================================
  // 소셜 로그인 연동
  // ========================================

  /**
   * 소셜 로그인을 연동한다.
   *
   * <p>소셜 로그인은 CUSTOMER 유형만 허용된다.
   *
   * @param providerType 소셜 로그인 제공자
   * @param providerUserId 제공자 측 사용자 ID
   * @throws AuthException CUSTOMER가 아니거나, 계정이 탈퇴 상태이거나, 이미 연동된 제공자인 경우
   */
  public void connectProvider(ProviderType providerType, String providerUserId) {
    validateOAuthAllowedForUserType(this.userType);
    validateNotWithdrawn();

    boolean alreadyConnected =
        this.providers.stream().anyMatch(p -> p.getProvider() == providerType);

    if (alreadyConnected) {
      throw new AuthException(AuthErrorCode.PROVIDER_ALREADY_CONNECTED, providerType.name());
    }

    AuthProvider provider = AuthProvider.create(this, providerType, providerUserId);
    this.providers.add(provider);
  }

  /**
   * 소셜 로그인을 연동한다 (내부용, 검증 없음).
   *
   * <p>registerWithOAuth에서 사용. 외부에서는 connectProvider를 사용해야 한다.
   */
  private void addProviderInternal(ProviderType providerType, String providerUserId) {
    AuthProvider provider = AuthProvider.create(this, providerType, providerUserId);
    this.providers.add(provider);
  }

  /**
   * 소셜 로그인 연동을 해제한다.
   *
   * <p>비밀번호가 필수이므로 모든 소셜 연동을 해제해도 일반 로그인이 가능하다.
   *
   * @param providerType 해제할 소셜 로그인 제공자
   * @throws AuthException 계정이 탈퇴 상태인 경우
   */
  public void disconnectProvider(ProviderType providerType) {
    validateNotWithdrawn();

    this.providers.removeIf(p -> p.getProvider() == providerType);
  }

  /**
   * 특정 소셜 로그인 제공자가 연동되어 있는지 확인한다.
   *
   * @param providerType 확인할 제공자
   * @return 연동되어 있으면 true
   */
  public boolean hasProvider(ProviderType providerType) {
    return this.providers.stream().anyMatch(p -> p.getProvider() == providerType);
  }

  // ========================================
  // 검증 메서드
  // ========================================

  private void validateEmail(String email) {
    if (email == null || email.isBlank()) {
      throw new AuthException(AuthErrorCode.INVALID_EMAIL);
    }
  }

  private void validateUserType(UserType userType) {
    if (userType == null) {
      throw new AuthException(AuthErrorCode.INVALID_USER_TYPE);
    }
  }

  private void validateCanLogin() {
    if (this.status.isLocked()) {
      throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED);
    }
    if (this.status.isWithdrawn()) {
      throw new AuthException(AuthErrorCode.ACCOUNT_WITHDRAWN);
    }
  }

  private void validateNotWithdrawn() {
    if (this.status.isWithdrawn()) {
      throw new AuthException(AuthErrorCode.ACCOUNT_WITHDRAWN);
    }
  }

  private static void validateOAuthAllowedForUserType(UserType userType) {
    if (!userType.isCustomer()) {
      throw new AuthException(AuthErrorCode.OAUTH_NOT_ALLOWED_FOR_USER_TYPE, userType.name());
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Auth auth)) {
      return false;
    }
    return Objects.equals(id, auth.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}