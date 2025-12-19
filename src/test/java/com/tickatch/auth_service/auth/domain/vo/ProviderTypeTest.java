package com.tickatch.auth_service.auth.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ProviderType 테스트")
class ProviderTypeTest {

  @Nested
  class Desription {

    @Test
    void 각_제공자는_한글_설명을_가진다() {

      assertThat(ProviderType.KAKAO.getDescription()).isEqualTo("카카오");
      assertThat(ProviderType.NAVER.getDescription()).isEqualTo("네이버");
      assertThat(ProviderType.GOOGLE.getDescription()).isEqualTo("구글");
    }
  }

  @Nested
  class Values_테스트 {

    @Test
    void 제공자가_3개_존재한다() {
      assertThat(ProviderType.values()).hasSize(3);
    }
  }
}
