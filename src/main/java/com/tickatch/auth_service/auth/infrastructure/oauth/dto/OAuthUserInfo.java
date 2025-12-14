package com.tickatch.auth_service.auth.infrastructure.oauth.dto;

import com.tickatch.auth_service.auth.domain.vo.ProviderType;

/**
 * OAuth 사용자 정보.
 *
 * <p>각 OAuth 제공자의 응답을 파싱하여 공통 형식으로 변환한 결과이다.
 *
 * @param providerType 제공자 타입
 * @param providerUserId 제공자 측 사용자 ID
 * @param email 이메일
 * @param name 이름 (닉네임)
 * @param profileImage 프로필 이미지 URL
 */
public record OAuthUserInfo(
    ProviderType providerType,
    String providerUserId,
    String email,
    String name,
    String profileImage
) {

  /**
   * 빌더 패턴으로 생성.
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private ProviderType providerType;
    private String providerUserId;
    private String email;
    private String name;
    private String profileImage;

    public Builder providerType(ProviderType providerType) {
      this.providerType = providerType;
      return this;
    }

    public Builder providerUserId(String providerUserId) {
      this.providerUserId = providerUserId;
      return this;
    }

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder profileImage(String profileImage) {
      this.profileImage = profileImage;
      return this;
    }

    public OAuthUserInfo build() {
      return new OAuthUserInfo(providerType, providerUserId, email, name, profileImage);
    }
  }
}