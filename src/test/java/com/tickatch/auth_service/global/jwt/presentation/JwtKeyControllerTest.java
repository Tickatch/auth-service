package com.tickatch.auth_service.global.jwt.presentation;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tickatch.auth_service.token.application.port.out.TokenProvider;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("JwtKeyController 테스트")
@WebMvcTest(JwtKeyController.class)
@AutoConfigureMockMvc(addFilters = false)
class JwtKeyControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private TokenProvider tokenProvider;

  private RSAPublicKey publicKey;

  @BeforeEach
  void setUp() throws Exception {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    publicKey = (RSAPublicKey) keyPair.getPublic();
  }

  @Nested
  @DisplayName("GET /.well-known/jwks.json")
  class GetJwks {

    @Test
    void JWKS_반환을_성공한다() throws Exception {
      given(tokenProvider.getPublicKey()).willReturn(publicKey);
      given(tokenProvider.getKeyId()).willReturn("test-key-id");

      mockMvc
          .perform(get("/.well-known/jwks.json"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.keys").isArray())
          .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
          .andExpect(jsonPath("$.keys[0].alg").value("RS256"))
          .andExpect(jsonPath("$.keys[0].use").value("sig"))
          .andExpect(jsonPath("$.keys[0].kid").value("test-key-id"))
          .andExpect(jsonPath("$.keys[0].n").exists())
          .andExpect(jsonPath("$.keys[0].e").exists());
    }
  }

  @Nested
  @DisplayName("GET /.well-known/public-key.pem")
  class GetPublicKeyPem {

    @Test
    void PEM_형식_Public_Key_반환을_성공한다() throws Exception {
      given(tokenProvider.getPublicKey()).willReturn(publicKey);

      mockMvc
          .perform(get("/.well-known/public-key.pem"))
          .andExpect(status().isOk())
          .andExpect(
              content().string(org.hamcrest.Matchers.containsString("-----BEGIN PUBLIC KEY-----")))
          .andExpect(
              content().string(org.hamcrest.Matchers.containsString("-----END PUBLIC KEY-----")));
    }
  }
}
