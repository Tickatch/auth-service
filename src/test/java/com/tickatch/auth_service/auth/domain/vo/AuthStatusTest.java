package com.tickatch.auth_service.auth.domain.vo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AuthStatus 테스트")
class AuthStatusTest {

  @Nested
  class canChangeTo_테스트 {

    @Test
    void ACTIVE에서_LOCKED로_전이_가능하다() {
      assertThat(AuthStatus.ACTIVE.canChangeTo(AuthStatus.LOCKED)).isTrue();
    }

    @Test
    void ACTIVE에서_WITHDRAWN으로_전이_가능하다() {
      assertThat(AuthStatus.ACTIVE.canChangeTo(AuthStatus.WITHDRAWN)).isTrue();
    }

    @Test
    void LOCKED에서_ACTIVE로_전이_가능하다() {
      assertThat(AuthStatus.LOCKED.canChangeTo(AuthStatus.ACTIVE)).isTrue();
    }

    @Test
    void LOCKED에서_WITHDRAWN으로_전이_불가능하다() {
      assertThat(AuthStatus.LOCKED.canChangeTo(AuthStatus.WITHDRAWN)).isFalse();
    }

    @Test
    void WITHDRAWN으에서_다른_상태로_전이_불가능하다() {
      assertThat(AuthStatus.WITHDRAWN.canChangeTo(AuthStatus.ACTIVE)).isFalse();
      assertThat(AuthStatus.WITHDRAWN.canChangeTo(AuthStatus.LOCKED)).isFalse();
    }

    @Test
    void 같은_상태로_전이_불가능하다() {
      assertThat(AuthStatus.ACTIVE.canChangeTo(AuthStatus.ACTIVE)).isFalse();
    }

    @Test
    void null로_전이_불가능하다() {
      assertThat(AuthStatus.ACTIVE.canChangeTo(null)).isFalse();
    }
  }

  @Nested
  class canLogin_메서드 {

    @Test
    void ACTIVE_상태에서만_로그인_가능하다() {
      assertThat(AuthStatus.ACTIVE.canLogin()).isTrue();
      assertThat(AuthStatus.LOCKED.canLogin()).isFalse();
      assertThat(AuthStatus.WITHDRAWN.canLogin()).isFalse();
    }
  }

  @Nested
  class 상태_확인_메서드 {

    @Test
    void isActive는_ACTIVE_상태에서만_true를_반환한다() {

      assertThat(AuthStatus.ACTIVE.isActive()).isTrue();
      assertThat(AuthStatus.LOCKED.isActive()).isFalse();
      assertThat(AuthStatus.WITHDRAWN.isActive()).isFalse();
    }

    @Test
    void isLocked는_LOCKED_상태에서만_true를_반환한다() {

      assertThat(AuthStatus.LOCKED.isLocked()).isTrue();
      assertThat(AuthStatus.ACTIVE.isLocked()).isFalse();
      assertThat(AuthStatus.WITHDRAWN.isLocked()).isFalse();
    }

    @Test
    void isWithdrawn은_WITHDRAWN_상태에서만_true를_반환한다() {

      assertThat(AuthStatus.WITHDRAWN.isWithdrawn()).isTrue();
      assertThat(AuthStatus.ACTIVE.isWithdrawn()).isFalse();
      assertThat(AuthStatus.LOCKED.isWithdrawn()).isFalse();
    }

    @Test
    void isTerminal은_WITHDRAWN_상태에서만_true를_반환한다() {

      assertThat(AuthStatus.WITHDRAWN.isTerminal()).isTrue();
      assertThat(AuthStatus.ACTIVE.isTerminal()).isFalse();
      assertThat(AuthStatus.LOCKED.isTerminal()).isFalse();
    }
  }

  @Nested
  class Description {

    @Test
    void 각_상태는_한글_설명을_가진다() {

      assertThat(AuthStatus.ACTIVE.getDescription()).isEqualTo("활성");
      assertThat(AuthStatus.LOCKED.getDescription()).isEqualTo("잠금");
      assertThat(AuthStatus.WITHDRAWN.getDescription()).isEqualTo("탈퇴");
    }
  }
}
