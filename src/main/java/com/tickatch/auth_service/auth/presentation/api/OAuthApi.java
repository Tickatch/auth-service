package com.tickatch.auth_service.auth.presentation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickatch.auth_service.auth.application.service.command.OAuthCommandService;
import com.tickatch.auth_service.auth.application.service.command.dto.LoginResult;
import com.tickatch.auth_service.auth.domain.exception.AuthErrorCode;
import com.tickatch.auth_service.auth.domain.exception.AuthException;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.infrastructure.oauth.OAuthProperties;
import com.tickatch.auth_service.auth.presentation.api.dto.response.LoginResponse;
import io.github.tickatch.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth 인증 API.
 *
 * <p>소셜 로그인 (Kakao, Naver, Google) 엔드포인트를 제공한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Tag(name = "OAuth", description = "소셜 로그인 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/oauth")
@RequiredArgsConstructor
public class OAuthApi {

  private final OAuthCommandService oAuthCommandService;
  private final OAuthProperties oAuthProperties;
  private final ObjectMapper objectMapper;

  /**
   * OAuth 로그인 URL로 리다이렉트.
   *
   * <p>사용자를 소셜 로그인 제공자의 인증 페이지로 리다이렉트한다. OAuth는 CUSTOMER 전용이다.
   *
   * @param provider 소셜 로그인 제공자 (kakao, naver, google)
   * @param rememberMe 로그인 유지 여부
   * @param userAgent 디바이스 정보
   * @param response HTTP 응답 (리다이렉트용)
   */
  @Operation(summary = "OAuth 로그인", description = "소셜 로그인 페이지로 리다이렉트합니다. (CUSTOMER 전용)")
  @GetMapping("/{provider}")
  public void redirectToOAuth(
      @Parameter(description = "소셜 로그인 제공자", example = "kakao")
      @PathVariable String provider,

      @Parameter(description = "로그인 유지 여부")
      @RequestParam(defaultValue = "false") boolean rememberMe,

      @Parameter(hidden = true)
      @RequestHeader(value = "User-Agent", defaultValue = "Unknown") String userAgent,

      HttpServletResponse response
  ) throws IOException {
    ProviderType providerType = parseProviderType(provider);

    String authorizationUrl = oAuthCommandService.getAuthorizationUrl(
        providerType, rememberMe, userAgent);

    log.info("OAuth 로그인 리다이렉트 - provider: {}", provider);
    response.sendRedirect(authorizationUrl);
  }

  /**
   * OAuth 콜백 처리.
   *
   * <p>소셜 로그인 제공자에서 인증 후 리다이렉트되는 콜백을 처리한다.
   * <p>인증 결과를 프론트엔드 콜백 페이지로 리다이렉트하여 전달한다.
   *
   * @param provider 소셜 로그인 제공자
   * @param code 인가 코드
   * @param state 상태 값 (userType, rememberMe, deviceInfo 포함)
   * @param error 에러 코드 (사용자가 로그인 취소 시)
   * @param response HttpServletResponse
   */
  @Operation(summary = "OAuth 콜백", description = "소셜 로그인 콜백을 처리합니다.")
  @GetMapping("/{provider}/callback")
  public void handleOAuthCallback(
      @Parameter(description = "소셜 로그인 제공자", example = "kakao")
      @PathVariable String provider,

      @Parameter(description = "인가 코드")
      @RequestParam(required = false) String code,

      @Parameter(description = "상태 값")
      @RequestParam(required = false) String state,

      @Parameter(description = "에러 코드 (로그인 취소 시)")
      @RequestParam(required = false) String error,

      HttpServletResponse response
  ) throws IOException {

    // 프론트엔드 콜백 URL (환경변수로 관리 권장)
    String frontendCallbackUrl = "http://localhost:3000/oauth/callback";

    // 사용자가 로그인 취소
    if (error != null) {
      log.info("OAuth 로그인 취소 - provider: {}, error: {}", provider, error);
      String errorMessage = URLEncoder.encode("로그인이 취소되었습니다.", StandardCharsets.UTF_8);
      response.sendRedirect(frontendCallbackUrl + "?error=" + errorMessage);
      return;
    }

    // 인가 코드 필수
    if (code == null || code.isBlank()) {
      String errorMessage = URLEncoder.encode("인증 코드가 유효하지 않습니다.", StandardCharsets.UTF_8);
      response.sendRedirect(frontendCallbackUrl + "?error=" + errorMessage);
      return;
    }

    try {
      ProviderType providerType = parseProviderType(provider);
      LoginResult result = oAuthCommandService.handleCallback(providerType, code, state);
      log.info("OAuth 콜백 처리 완료 - provider: {}, authId: {}", provider, result.authId());

      // ApiResponse로 래핑하여 JSON 변환
      ApiResponse<LoginResponse> apiResponse = ApiResponse.success(LoginResponse.from(result));
      String jsonData = objectMapper.writeValueAsString(apiResponse);
      String encodedData = URLEncoder.encode(jsonData, StandardCharsets.UTF_8);

      response.sendRedirect(frontendCallbackUrl + "?success=true&data=" + encodedData);

    } catch (AuthException e) {
      log.error("OAuth 인증 실패 - provider: {}, error: {}", provider, e.getMessage());
      String errorMessage = URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8);
      response.sendRedirect(frontendCallbackUrl + "?error=" + errorMessage);

    } catch (Exception e) {
      log.error("OAuth 콜백 처리 중 오류 - provider: {}", provider, e);
      String errorMessage = URLEncoder.encode("인증 처리 중 오류가 발생했습니다.", StandardCharsets.UTF_8);
      response.sendRedirect(frontendCallbackUrl + "?error=" + errorMessage);
    }
  }

  /**
   * 소셜 계정 연동.
   *
   * <p>기존 계정에 소셜 로그인을 연동한다.
   *
   * @param provider 소셜 로그인 제공자
   * @param authId 인증된 사용자 ID
   * @param userAgent 디바이스 정보
   * @param response HTTP 응답 (리다이렉트용)
   */
  @Operation(summary = "소셜 계정 연동", description = "기존 계정에 소셜 로그인을 연동합니다.")
  @GetMapping("/{provider}/link")
  public void linkSocialAccount(
      @Parameter(description = "소셜 로그인 제공자", example = "kakao")
      @PathVariable String provider,

      @Parameter(description = "인증된 사용자 ID (Gateway에서 주입)")
      @RequestHeader("X-User-Id") UUID authId,

      @Parameter(hidden = true)
      @RequestHeader(value = "User-Agent", defaultValue = "Unknown") String userAgent,

      HttpServletResponse response
  ) throws IOException {
    ProviderType providerType = parseProviderType(provider);

    String linkUrl = oAuthCommandService.getLinkUrl(providerType, authId, userAgent);

    log.info("소셜 계정 연동 리다이렉트 - provider: {}, authId: {}", provider, authId);
    response.sendRedirect(linkUrl);
  }

  /**
   * 소셜 계정 연동 해제.
   */
  @Operation(summary = "소셜 계정 연동 해제", description = "연동된 소셜 계정을 해제합니다.")
  @DeleteMapping("/{provider}/unlink")
  public ResponseEntity<ApiResponse<Void>> unlinkSocialAccount(
      @Parameter(description = "소셜 로그인 제공자", example = "kakao")
      @PathVariable String provider,

      @Parameter(description = "인증된 사용자 ID (Gateway에서 주입)")
      @RequestHeader("X-User-Id") UUID authId
  ) {
    ProviderType providerType = parseProviderType(provider);

    oAuthCommandService.unlinkProvider(authId, providerType);
    log.info("소셜 계정 연동 해제 완료 - provider: {}, authId: {}", provider, authId);

    return ResponseEntity.ok(ApiResponse.successWithMessage("소셜 계정 연동이 해제되었습니다."));
  }

  /**
   * 제공자 타입 파싱.
   */
  private ProviderType parseProviderType(String provider) {
    try {
      return ProviderType.valueOf(provider.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new AuthException(AuthErrorCode.INVALID_PROVIDER);
    }
  }
}