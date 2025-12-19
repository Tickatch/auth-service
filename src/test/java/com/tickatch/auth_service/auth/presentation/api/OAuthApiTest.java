package com.tickatch.auth_service.auth.presentation.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickatch.auth_service.auth.application.service.command.OAuthCommandService;
import com.tickatch.auth_service.auth.application.service.command.dto.LoginResult;
import com.tickatch.auth_service.auth.domain.exception.AuthException;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import com.tickatch.auth_service.auth.infrastructure.oauth.OAuthProperties;
import com.tickatch.auth_service.token.application.service.command.dto.TokenResult;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("OAuthApi 테스트")
@WebMvcTest(OAuthApi.class)
@AutoConfigureMockMvc(addFilters = false)
class OAuthApiTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private OAuthCommandService oAuthCommandService;

  @MockitoBean private OAuthProperties oAuthProperties;

  @Autowired private ObjectMapper objectMapper;

  private LoginResult createLoginResult() {
    UUID authId = UUID.randomUUID();
    TokenResult tokenResult =
        TokenResult.of(
            "access-token",
            "refresh-token",
            LocalDateTime.now().plusMinutes(5),
            LocalDateTime.now().plusDays(7));
    return LoginResult.of(authId, "test@test.com", UserType.CUSTOMER, tokenResult);
  }

  @Nested
  @DisplayName("GET /api/v1/auth/oauth/{provider}")
  class RedirectToOAuth_테스트 {

    @Test
    void 카카오_로그인_리다이렉트가_성공한다() throws Exception {
      String authUrl = "https://kauth.kakao.com/oauth/authorize?client_id=xxx";

      given(oAuthCommandService.getAuthorizationUrl(eq(ProviderType.KAKAO), eq(false), anyString()))
          .willReturn(authUrl);

      mockMvc
          .perform(get("/api/v1/auth/oauth/kakao"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl(authUrl));
    }

    @Test
    void 네이버_로그인_리다이렉트가_성공한다() throws Exception {
      String authUrl = "https://nid.naver.com/oauth2.0/authorize?client_id=xxx";

      given(oAuthCommandService.getAuthorizationUrl(eq(ProviderType.NAVER), eq(false), anyString()))
          .willReturn(authUrl);

      mockMvc
          .perform(get("/api/v1/auth/oauth/naver"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl(authUrl));
    }

    @Test
    void 구글_로그인_리다이렉트가_성공한다() throws Exception {
      String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id=xxx";

      given(
              oAuthCommandService.getAuthorizationUrl(
                  eq(ProviderType.GOOGLE), eq(false), anyString()))
          .willReturn(authUrl);

      mockMvc
          .perform(get("/api/v1/auth/oauth/google"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl(authUrl));
    }

    @Test
    void rememberMe_파라미터_전달에_성공한다() throws Exception {
      String authUrl = "https://kauth.kakao.com/oauth/authorize?client_id=xxx";

      given(oAuthCommandService.getAuthorizationUrl(eq(ProviderType.KAKAO), eq(true), anyString()))
          .willReturn(authUrl);

      mockMvc
          .perform(get("/api/v1/auth/oauth/kakao").param("rememberMe", "true"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl(authUrl));
    }
  }

  @Nested
  @DisplayName("GET /api/v1/auth/oauth/{provider}/callback")
  class HandleCallback_테스트 {

    @Test
    void 콜백_처리에_성공하면_프론트엔드로_리다이렉트한다() throws Exception {
      given(
              oAuthCommandService.handleCallback(
                  eq(ProviderType.KAKAO), eq("auth-code"), eq("state-value")))
          .willReturn(createLoginResult());

      mockMvc
          .perform(
              get("/api/v1/auth/oauth/kakao/callback")
                  .param("code", "auth-code")
                  .param("state", "state-value"))
          .andExpect(status().is3xxRedirection())
          .andExpect(
              redirectedUrlPattern("http://localhost:3000/oauth/callback?success=true&data=*"));
    }

    @Test
    void 로그인_취소_시_에러와_함께_리다이렉트한다() throws Exception {
      mockMvc
          .perform(get("/api/v1/auth/oauth/kakao/callback").param("error", "access_denied"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrlPattern("http://localhost:3000/oauth/callback?error=*"));
    }

    @Test
    void code가_없으면_에러와_함께_리다이렉트한다() throws Exception {
      mockMvc
          .perform(get("/api/v1/auth/oauth/kakao/callback").param("state", "state-value"))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrlPattern("http://localhost:3000/oauth/callback?error=*"));
    }
  }

  @Nested
  @DisplayName("GET /api/v1/auth/oauth/{provider}/link")
  class LinkSocialAccount_테스트 {

    @Test
    void 계정_연동_리다이렉트에_성공한다() throws Exception {
      UUID authId = UUID.randomUUID();
      String linkUrl = "https://kauth.kakao.com/oauth/authorize?client_id=xxx&state=link";

      given(oAuthCommandService.getLinkUrl(eq(ProviderType.KAKAO), eq(authId), anyString()))
          .willReturn(linkUrl);

      mockMvc
          .perform(get("/api/v1/auth/oauth/kakao/link").header("X-user-Id", authId.toString()))
          .andExpect(status().is3xxRedirection())
          .andExpect(redirectedUrl(linkUrl));
    }
  }

  @Nested
  @DisplayName("DELETE /api/v1/auth/oauth/{provider}/unlink")
  class UnlinkSocialAccount_테스트 {

    @Test
    void 연동_해제에_성공한다() throws Exception {
      UUID authId = UUID.randomUUID();

      doNothing().when(oAuthCommandService).unlinkProvider(eq(authId), eq(ProviderType.KAKAO));

      mockMvc
          .perform(delete("/api/v1/auth/oauth/kakao/unlink").header("X-user-Id", authId.toString()))
          .andExpect(status().isNoContent());
    }
  }

  @Nested
  class 잘못된_Provider_테스트 {

    @Test
    void 지원하지_않는_provider는_예외를_발생시킨다() {
      assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/auth/oauth/invalid")))
          .hasCauseInstanceOf(AuthException.class);
    }
  }
}
