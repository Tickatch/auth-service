package com.tickatch.auth_service.auth.application.service.command.dto;

import java.util.UUID;

/**
 * 회원탈퇴 요청 커맨드.
 *
 * @param authId Auth ID
 * @param password 비밀번호 (본인 확인용)
 */
public record WithdrawCommand(
    UUID authId,
    String password
) {

  public static WithdrawCommand of(UUID authId, String password) {
    return new WithdrawCommand(authId, password);
  }
}