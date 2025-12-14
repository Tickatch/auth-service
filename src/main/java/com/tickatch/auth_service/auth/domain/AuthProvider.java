package com.tickatch.auth_service.auth.domain;

import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 소셜 로그인 연동 정보 엔티티.
 *
 * <p>하나의 Auth에 여러 소셜 계정을 연동할 수 있으며, 동일한 제공자(Provider)는 한 번만 연동 가능하다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Entity
@Table(
    name = "auth_providers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_auth_provider", columnNames = {"auth_id", "provider"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthProvider {

  /* 소셜 연동 ID */
  @Id
  @Column(name = "id", columnDefinition = "uuid")
  private UUID id;

  /* 연동된 인증 정보 */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "auth_id", nullable = false)
  private Auth auth;

  /* 소셜 로그인 제공자 */
  @Enumerated(EnumType.STRING)
  @Column(name = "provider", nullable = false, length = 20)
  private ProviderType provider;

  /* 제공자 측 사용자 ID */
  @Column(name = "provider_user_id", nullable = false)
  private String providerUserId;

  /* 연동 일시 */
  @Column(name = "connected_at", nullable = false)
  private LocalDateTime connectedAt;

  private AuthProvider(Auth auth, ProviderType provider, String providerUserId) {
    this.id = UUID.randomUUID();
    this.auth = auth;
    this.provider = provider;
    this.providerUserId = providerUserId;
    this.connectedAt = LocalDateTime.now();
  }

  /**
   * 소셜 로그인 연동 정보를 생성한다.
   *
   * @param auth 연동할 Auth
   * @param provider 소셜 로그인 제공자
   * @param providerUserId 제공자 측 사용자 ID
   * @return 생성된 AuthProvider
   */
  public static AuthProvider create(Auth auth, ProviderType provider, String providerUserId) {
    return new AuthProvider(auth, provider, providerUserId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AuthProvider that)) {
      return false;
    }
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}