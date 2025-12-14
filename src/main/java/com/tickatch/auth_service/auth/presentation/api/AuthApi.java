package com.tickatch.auth_service.auth.presentation.api;

import com.tickatch.auth_service.auth.application.service.command.AuthCommandService;
import com.tickatch.auth_service.auth.application.service.command.dto.LoginResult;
import com.tickatch.auth_service.auth.application.service.query.AuthQueryService;
import com.tickatch.auth_service.auth.application.service.query.dto.AuthInfo;
import com.tickatch.auth_service.auth.presentation.api.dto.request.ChangePasswordRequest;
import com.tickatch.auth_service.auth.presentation.api.dto.request.CheckEmailRequest;
import com.tickatch.auth_service.auth.presentation.api.dto.request.LoginRequest;
import com.tickatch.auth_service.auth.presentation.api.dto.request.LogoutRequest;
import com.tickatch.auth_service.auth.presentation.api.dto.request.RefreshRequest;
import com.tickatch.auth_service.auth.presentation.api.dto.request.RegisterRequest;
import com.tickatch.auth_service.auth.presentation.api.dto.request.WithdrawRequest;
import com.tickatch.auth_service.auth.presentation.api.dto.response.AuthInfoResponse;
import com.tickatch.auth_service.auth.presentation.api.dto.response.CheckEmailResponse;
import com.tickatch.auth_service.auth.presentation.api.dto.response.LoginResponse;
import io.github.tickatch.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API 컨트롤러.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthApi {

  private final AuthCommandService authCommandService;
  private final AuthQueryService authQueryService;

  /**
   * 회원가입.
   */
  @Operation(summary = "회원가입", description = "이메일/비밀번호로 회원가입합니다.")
  @PostMapping("/register")
  public ResponseEntity<ApiResponse<LoginResponse>> register(
      @Valid @RequestBody RegisterRequest request,
      @Parameter(hidden = true) @RequestHeader(value = "User-Agent", defaultValue = "Unknown") String userAgent
  ) {
    LoginResult result = authCommandService.register(request.toCommand(userAgent));
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success(LoginResponse.from(result), "회원가입이 완료되었습니다."));
  }

  /**
   * 로그인.
   */
  @Operation(summary = "로그인", description = "이메일/비밀번호로 로그인합니다.")
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<LoginResponse>> login(
      @Valid @RequestBody LoginRequest request,
      @Parameter(hidden = true) @RequestHeader(value = "User-Agent", defaultValue = "Unknown") String userAgent
  ) {
    LoginResult result = authCommandService.login(request.toCommand(userAgent));
    return ResponseEntity.ok(ApiResponse.success(LoginResponse.from(result)));
  }

  /**
   * 토큰 갱신.
   */
  @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새 토큰을 발급받습니다.")
  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<LoginResponse>> refresh(
      @Valid @RequestBody RefreshRequest request
  ) {
    LoginResult result = authCommandService.refresh(request.toCommand());
    return ResponseEntity.ok(ApiResponse.success(LoginResponse.from(result)));
  }

  /**
   * 로그아웃.
   */
  @Operation(summary = "로그아웃", description = "현재 기기 또는 모든 기기에서 로그아웃합니다.")
  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      @RequestBody LogoutRequest request,
      @Parameter(description = "인증된 사용자 ID (Gateway에서 주입)")
      @RequestHeader("X-User-Id") UUID authId
  ) {
    authCommandService.logout(request.toCommand(authId));
    return ResponseEntity.ok(ApiResponse.successWithMessage("로그아웃되었습니다."));
  }

  /**
   * 비밀번호 변경.
   */
  @Operation(summary = "비밀번호 변경", description = "비밀번호를 변경합니다. 변경 후 모든 기기에서 로그아웃됩니다.")
  @PutMapping("/password")
  public ResponseEntity<ApiResponse<Void>> changePassword(
      @Valid @RequestBody ChangePasswordRequest request,
      @Parameter(description = "인증된 사용자 ID (Gateway에서 주입)")
      @RequestHeader("X-User-Id") UUID authId
  ) {
    authCommandService.changePassword(request.toCommand(authId));
    return ResponseEntity.ok(ApiResponse.successWithMessage("비밀번호가 변경되었습니다."));
  }

  /**
   * 회원탈퇴.
   */
  @Operation(summary = "회원탈퇴", description = "회원탈퇴합니다. 모든 데이터가 삭제됩니다.")
  @DeleteMapping("/withdraw")
  public ResponseEntity<ApiResponse<Void>> withdraw(
      @Valid @RequestBody WithdrawRequest request,
      @Parameter(description = "인증된 사용자 ID (Gateway에서 주입)")
      @RequestHeader("X-User-Id") UUID authId
  ) {
    authCommandService.withdraw(request.toCommand(authId));
    return ResponseEntity.ok(ApiResponse.successWithMessage("회원탈퇴가 완료되었습니다."));
  }

  /**
   * 내 정보 조회.
   */
  @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 정보를 조회합니다.")
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<AuthInfoResponse>> getMyInfo(
      @Parameter(description = "인증된 사용자 ID (Gateway에서 주입)")
      @RequestHeader("X-User-Id") UUID authId
  ) {
    AuthInfo info = authQueryService.findById(authId);
    return ResponseEntity.ok(ApiResponse.success(AuthInfoResponse.from(info)));
  }

  /**
   * 이메일 중복 확인.
   */
  @Operation(summary = "이메일 중복 확인", description = "이메일 사용 가능 여부를 확인합니다.")
  @PostMapping("/check-email")
  public ResponseEntity<ApiResponse<CheckEmailResponse>> checkEmail(
      @Valid @RequestBody CheckEmailRequest request
  ) {
    boolean exists = authQueryService.existsByEmailAndUserType(request.email(), request.userType());
    return ResponseEntity.ok(ApiResponse.success(CheckEmailResponse.of(exists)));
  }
}