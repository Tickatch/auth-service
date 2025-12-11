package com.tickatch.auth_service.global.jwt.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("RsaKeyManager 테스트")
class RsaKeyManagerTest {

  @TempDir
  Path tempDir;

  @Nested
  class 키_자동_생성_테스트 {

    @Test
    void 키가_없으면_자동으로_생성된다() {
      JwtProperties properties = new JwtProperties();
      properties.setKeyDirectory(tempDir.toString());
      properties.setKeyId("test-key");

      RsaKeyManager keyManager = new RsaKeyManager(properties);

      keyManager.init();

      assertThat(keyManager.getPrivateKey()).isNotNull();
      assertThat(keyManager.getPublicKey()).isNotNull();
      assertThat(keyManager.getPrivateKey().getAlgorithm()).isEqualTo("RSA");
      assertThat(keyManager.getPublicKey().getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void 생성된_키_파일이_저장된다() {
      JwtProperties properties = new JwtProperties();
      properties.setKeyDirectory(tempDir.toString());
      properties.setKeyId("test-key");

      RsaKeyManager keyManager = new RsaKeyManager(properties);

      keyManager.init();

      assertThat(Files.exists(tempDir.resolve("private.pem"))).isTrue();
      assertThat(Files.exists(tempDir.resolve("public.pem"))).isTrue();
    }
  }

  @Nested
  class PublicKeyBase64_테스트 {

    @Test
    void Public_Key를_Base64로_인코딩한다() {
      JwtProperties properties = new JwtProperties();
      properties.setKeyDirectory(tempDir.toString());
      properties.setKeyId("test-key");

      RsaKeyManager keyManager = new RsaKeyManager(properties);
      keyManager.init();

      String base64 = keyManager.getPublicKeyBase64();

      assertThat(base64).isNotNull();
      assertThat(base64).isNotBlank();
    }
  }

  @Nested
  class 키_파일_로딩_테스트 {

    @Test
    void 기존_키_파일이_있으면_로딩한다() {
      JwtProperties properties = new JwtProperties();
      properties.setKeyDirectory(tempDir.toString());
      properties.setKeyId("test-key");

      RsaKeyManager keyManager1 = new RsaKeyManager(properties);
      keyManager1.init();
      String originalPublicKey = keyManager1.getPublicKeyBase64();

      RsaKeyManager keyManager2 = new RsaKeyManager(properties);
      keyManager2.init();

      assertThat(keyManager2.getPublicKeyBase64()).isEqualTo(originalPublicKey);
    }
  }
}