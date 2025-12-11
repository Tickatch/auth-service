package com.tickatch.auth_service.auth.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UserType 테스트")
class UserTypeTest {

  @Nested
  class 타입_확인_메서드 {

    @Test
    void isCustomer는_CUSTOMER_타입에서만_true를_반환한다() {

      assertThat(UserType.CUSTOMER.isCustomer()).isTrue();
      assertThat(UserType.SELLER.isCustomer()).isFalse();
      assertThat(UserType.ADMIN.isCustomer()).isFalse();
    }

    @Test
    void isSeller는_SELLER_타입에서만_true를_반환한다() {

      assertThat(UserType.SELLER.isSeller()).isTrue();
      assertThat(UserType.CUSTOMER.isSeller()).isFalse();
      assertThat(UserType.ADMIN.isSeller()).isFalse();
    }

    @Test
    void isAdmin는_ADMIN_타입에서만_true를_반환한다() {

      assertThat(UserType.ADMIN.isAdmin()).isTrue();
      assertThat(UserType.CUSTOMER.isAdmin()).isFalse();
      assertThat(UserType.SELLER.isAdmin()).isFalse();
    }
  }

  @Nested
  class Description {

    @Test
    void 각_타입은_한글_설명을_가진다() {

      assertThat(UserType.CUSTOMER.getDescription()).isEqualTo("구매자");
      assertThat(UserType.SELLER.getDescription()).isEqualTo("판매자");
      assertThat(UserType.ADMIN.getDescription()).isEqualTo("관리자");
    }
  }
}