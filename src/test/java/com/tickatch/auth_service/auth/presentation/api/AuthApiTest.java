package com.tickatch.auth_service.auth.presentation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickatch.auth_service.auth.application.service.command.AuthCommandService;
import com.tickatch.auth_service.auth.application.service.command.dto.LoginResult;
import com.tickatch.auth_service.auth.application.service.query.AuthQueryService;
import com.tickatch.auth_service.auth.application.service.query.dto.AuthInfo;
import com.tickatch.auth_service.auth.domain.vo.AuthStatus;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import com.tickatch.auth_service.auth.presentation.api.dto.request.ChangePasswordRequest;
import com.tickatch.auth_service.auth.presentation.api.dto.request.CheckEmailRequest;
import com.tickatch.auth_service.auth.presentation.api.dto.request.LoginRequest;
import com.tickatch.auth_service.auth.presentation.api.dto.request.LogoutRequest;
import com.tickatch.auth_service.auth.presentation.api.dto.request.RefreshRequest;
import com.tickatch.auth_service.auth.presentation.api.dto.request.RegisterRequest;
import com.tickatch.auth_service.auth.presentation.api.dto.request.WithdrawRequest;
import com.tickatch.auth_service.token.application.service.command.dto.TokenResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("AuthApi 테스트")
@WebMvcTest(AuthApi.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthApiTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private AuthCommandService authCommandService;

  @MockitoBean
  private AuthQueryService authQueryService;

  private LoginResult createLoginResult() {
    UUID authId = UUID.randomUUID();
    TokenResult tokenResult = TokenResult.of(
        "access-token",
        "refresh-token",
        LocalDateTime.now().plusMinutes(5),
        LocalDateTime.now().plusDays(7)
    );
    return LoginResult.of(authId, "test@test.com", UserType.CUSTOMER, tokenResult);
  }

  @Nested
  @DisplayName("POST /api/v1/auth/register")
  class Register {

    @Test
    void 회원가입을_성공한다() throws Exception {
      RegisterRequest request = new RegisterRequest(
          "test@test.com", "Password123!", UserType.CUSTOMER, false);

      given(authCommandService.register(any())).willReturn(createLoginResult());

      mockMvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.email").value("test@test.com"))
          .andExpect(jsonPath("$.accessToken").value("access-token"));
    }

    @Test
    void 이메일_누락_시_400_에러가_발생한다() throws Exception {
      RegisterRequest request = new RegisterRequest(
          "", "Password123!", UserType.CUSTOMER, false);

      mockMvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }

    @Test
    void 비밀번호_짧으면_400_에러가_발생한다() throws Exception {
      RegisterRequest request = new RegisterRequest(
          "test@test.com", "short", UserType.CUSTOMER, false);

      mockMvc.perform(post("/api/v1/auth/register")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("POST /api/v1/auth/login")
  class Login {

    @Test
    void 로그인_성공에_성공한다() throws Exception {

      LoginRequest request = new LoginRequest(
          "test@test.com", "Password123!", UserType.CUSTOMER, false);

      given(authCommandService.login(any())).willReturn(createLoginResult());

      mockMvc.perform(post("/api/v1/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.email").value("test@test.com"))
          .andExpect(jsonPath("$.accessToken").value("access-token"))
          .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/auth/refresh")
  class Refresh {

    @Test
    void 토큰_갱신_성공한다() throws Exception {
      RefreshRequest request = new RefreshRequest("refresh-token");

      given(authCommandService.refresh(any())).willReturn(createLoginResult());

      mockMvc.perform(post("/api/v1/auth/refresh")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessToken").value("access-token"));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/auth/logout")
  class Logout {

    @Test
    void 로그아웃을_성공한다() throws Exception {
      UUID authId = UUID.randomUUID();
      LogoutRequest request = new LogoutRequest("refresh-token", false);

      doNothing().when(authCommandService).logout(any());

      mockMvc.perform(post("/api/v1/auth/logout")
              .contentType(MediaType.APPLICATION_JSON)
              .header("X-User-Id", authId.toString())
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNoContent());
    }
  }

  @Nested
  @DisplayName("PUT /api/v1/auth/password")
  class ChangePassword {

    @Test
    void 비밀번호_변경에_성공한다() throws Exception {
      UUID authId = UUID.randomUUID();
      ChangePasswordRequest request = new ChangePasswordRequest(
          "CurrentPassword123!", "NewPassword456!");

      doNothing().when(authCommandService).changePassword(any());

      mockMvc.perform(put("/api/v1/auth/password")
              .contentType(MediaType.APPLICATION_JSON)
              .header("X-User-Id", authId.toString())
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNoContent());
    }
  }

  @Nested
  @DisplayName("DELETE /api/v1/auth/withdraw")
  class Withdraw {

    @Test
    void 회원탈퇴_성공한다() throws Exception {
      UUID authId = UUID.randomUUID();
      WithdrawRequest request = new WithdrawRequest("Password123!");

      doNothing().when(authCommandService).withdraw(any());

      mockMvc.perform(delete("/api/v1/auth/withdraw")
              .contentType(MediaType.APPLICATION_JSON)
              .header("X-User-Id", authId.toString())
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isNoContent());
    }
  }

  @Nested
  @DisplayName("GET /api/v1/auth/me")
  class GetMyInfo {

    @Test
    void 내_정보_조회할_수_있다() throws Exception {
      UUID authId = UUID.randomUUID();
      AuthInfo authInfo = new AuthInfo(
          authId, "test@test.com", UserType.CUSTOMER, AuthStatus.ACTIVE,
          LocalDateTime.now(), List.of(), LocalDateTime.now());

      given(authQueryService.findById(authId)).willReturn(authInfo);

      mockMvc.perform(get("/api/v1/auth/me")
              .header("X-User-Id", authId.toString()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.email").value("test@test.com"))
          .andExpect(jsonPath("$.userType").value("CUSTOMER"));
    }
  }

  @Nested
  @DisplayName("POST /api/v1/auth/check-email")
  class CheckEmail {

    @Test
    void 이메일_사용_가능하다() throws Exception {
      CheckEmailRequest request = new CheckEmailRequest("new@test.com", UserType.CUSTOMER);

      given(authQueryService.existsByEmailAndUserType("new@test.com", UserType.CUSTOMER))
          .willReturn(false);

      mockMvc.perform(post("/api/v1/auth/check-email")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void 이메일이_이미_존재한다() throws Exception {
      CheckEmailRequest request = new CheckEmailRequest("exist@test.com", UserType.CUSTOMER);

      given(authQueryService.existsByEmailAndUserType("exist@test.com", UserType.CUSTOMER))
          .willReturn(true);

      mockMvc.perform(post("/api/v1/auth/check-email")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.available").value(false));
    }
  }
}