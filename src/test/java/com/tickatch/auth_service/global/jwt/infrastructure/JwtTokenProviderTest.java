package com.tickatch.auth_service.global.jwt.infrastructure;


import static org.assertj.core.api.Assertions.assertThat;

import com.tickatch.auth_service.auth.domain.vo.UserType;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

  private JwtTokenProvider jwtTokenProvider;
  private JwtProperties jwtProperties;
  private RsaKeyManager rsaKeyManager;

  @BeforeEach
  void setUp() throws Exception {
    jwtProperties = new JwtProperties();
    jwtProperties.setIssuer("tickatch-test");
    jwtProperties.setKeyId("test-key-1");
    jwtProperties.getAccessToken().setExpiration(300);
    rsaKeyManager = createTestRsaKeyManager();
    jwtTokenProvider = new JwtTokenProvider(jwtProperties, rsaKeyManager);
  }

  private RsaKeyManager createTestRsaKeyManager() throws Exception {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();

    return new TestRsaKeyManager(
        (RSAPrivateKey) keyPair.getPrivate(),
        (RSAPublicKey) keyPair.getPublic()
    );
  }

  @Nested
  class Access_Token_생성_테스트 {

    @Test
    void Access_Token을_생성한다() {
      UUID authId = UUID.randomUUID();
      UserType userType = UserType.CUSTOMER;

      String token = jwtTokenProvider.generateAccessToken(authId, userType);
      UUID extractedAuthId = jwtTokenProvider.extractAuthId(token);

      assertThat(extractedAuthId).isEqualTo(authId);
    }

    @Test
    void 생성된_토큰에서_authId를_추출할_수_있다() {
      UUID authId = UUID.randomUUID();
      UserType userType = UserType.SELLER;

      String token = jwtTokenProvider.generateAccessToken(authId, userType);
      UserType extractedUserType = jwtTokenProvider.extractUserType(token);

      assertThat(extractedUserType).isEqualTo(userType);
    }
  }

  @Nested
  class Refresh_Token_값_생성_테스트 {

    @Test
    void Refresh_Token_값을_생성한다() {
      String tokenValue = jwtTokenProvider.generateRefreshTokenValue();

      assertThat(tokenValue).isNotNull();
      assertThat(tokenValue).hasSize(64);
    }

    @Test
    void 매번_다른_Refresh_Token_값이_생성된다() {
      String token1 = jwtTokenProvider.generateRefreshTokenValue();
      String token2 = jwtTokenProvider.generateRefreshTokenValue();

      assertThat(token1).isNotEqualTo(token2);
    }
  }

  @Nested
  class 토큰_검증_테스트 {

    @Test
    void 유효한_토큰은_true를_반환한다() {
      UUID authId = UUID.randomUUID();
      String token = jwtTokenProvider.generateAccessToken(authId, UserType.CUSTOMER);

      boolean isValid = jwtTokenProvider.validateToken(token);

      assertThat(isValid).isTrue();
    }

    @Test
    void 잘못된_형식의_토큰은_false를_반환한다() {
      boolean isValid = jwtTokenProvider.validateToken("invalid-token");

      assertThat(isValid).isFalse();
    }

    @Test
    void 빈_토큰은_false를_반환한다() {
      boolean isValid = jwtTokenProvider.validateToken("");

      assertThat(isValid).isFalse();
    }

    @Test
    void 다른_키로_서명된_토큰은_false를_반환한다() throws Exception {
      RsaKeyManager otherKeyManager = createTestRsaKeyManager();
      JwtTokenProvider otherProvider = new JwtTokenProvider(jwtProperties, otherKeyManager);

      UUID authId = UUID.randomUUID();
      String token = otherProvider.generateAccessToken(authId, UserType.CUSTOMER);

      boolean isValid = jwtTokenProvider.validateToken(token);

      assertThat(isValid).isFalse();
    }
  }

  @Nested
  class Public_key_조회_테스트 {

    @Test
    void publicKey를_반환한다() {
      RSAPublicKey publicKey = jwtTokenProvider.getPublicKey();

      assertThat(publicKey).isNotNull();
      assertThat(publicKey.getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void key_ID를_반환한다() {
      String keyId = jwtTokenProvider.getKeyId();

      assertThat(keyId).isEqualTo("test-key-1");
    }
  }

  private static class TestRsaKeyManager extends RsaKeyManager {
    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;

    TestRsaKeyManager(RSAPrivateKey privateKey, RSAPublicKey publicKey) {
      super(new JwtProperties());
      this.privateKey = privateKey;
      this.publicKey = publicKey;
    }

    @Override
    public RSAPrivateKey getPrivateKey() {
      return privateKey;
    }

    @Override
    public RSAPublicKey getPublicKey() {
      return publicKey;
    }
  }
}