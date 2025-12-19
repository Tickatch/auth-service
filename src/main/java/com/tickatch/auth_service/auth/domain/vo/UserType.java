package com.tickatch.auth_service.auth.domain.vo;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 유형.
 *
 * <p>동일한 이메일로 서로 다른 유형의 계정을 분리하여 가입할 수 있다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum UserType {

  /** 일반 구매자 */
  CUSTOMER("구매자"),

  /** 판매자 */
  SELLER("판매자"),

  /** 관리자 */
  ADMIN("관리자");

  /** 유형 설명 */
  private final String description;

  /**
   * 구매자인지 확인한다.
   *
   * @return CUSTOMER이면 true
   */
  public boolean isCustomer() {
    return this == CUSTOMER;
  }

  /**
   * 판매자인지 확인한다.
   *
   * @return SELLER이면 true
   */
  public boolean isSeller() {
    return this == SELLER;
  }

  /**
   * 관리자인지 확인한다.
   *
   * @return ADMIN이면 true
   */
  public boolean isAdmin() {
    return this == ADMIN;
  }
}
