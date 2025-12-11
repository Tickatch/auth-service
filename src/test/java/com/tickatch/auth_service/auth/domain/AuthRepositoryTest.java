package com.tickatch.auth_service.auth.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tickatch.auth_service.auth.domain.repository.AuthRepositoryImpl;
import com.tickatch.auth_service.auth.domain.repository.dto.AuthSearchCondition;
import com.tickatch.auth_service.auth.domain.vo.AuthStatus;
import com.tickatch.auth_service.auth.domain.vo.ProviderType;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("AuthRepository 통합 테스트")
class AuthRepositoryTest {

  @Autowired
  private AuthRepositoryImpl authRepository;

  private PasswordEncoder encoder;

  @BeforeEach
  void setUp() {
    encoder = new BCryptPasswordEncoder();
  }

  @Nested
  class 저장_테스트 {

    @Test
    void Auth를_저장하고_조회한다() {
      Auth auth = Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");

      Auth saved = authRepository.save(auth);

      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getEmail()).isEqualTo("test@example.com");
      assertThat(saved.getUserType()).isEqualTo(UserType.CUSTOMER);
    }

    @Test
    void ID로_Auth를_조회한다() {
      Auth auth = Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      Auth saved = authRepository.save(auth);

      Optional<Auth> found = authRepository.findById(saved.getId());

      assertThat(found).isPresent();
      assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }
  }

  @Nested
  class 이메일_사용자유형_조회_테스트 {

    @Test
    void 이메일과_사용자유형으로_Auth를_조회한다() {
      Auth auth = Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      authRepository.save(auth);

      Optional<Auth> found = authRepository.findByEmailAndUserType("test@example.com", UserType.CUSTOMER);

      assertThat(found).isPresent();
      assertThat(found.get().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    void 동일_이메일_다른_유형은_별도로_조회된다() {
      Auth customer = Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      Auth seller = Auth.register("test@example.com", "Pass123!", UserType.SELLER, encoder, "SYSTEM");
      authRepository.save(customer);
      authRepository.save(seller);

      Optional<Auth> foundCustomer = authRepository.findByEmailAndUserType("test@example.com", UserType.CUSTOMER);
      Optional<Auth> foundSeller = authRepository.findByEmailAndUserType("test@example.com", UserType.SELLER);

      assertThat(foundCustomer).isPresent();
      assertThat(foundSeller).isPresent();
      assertThat(foundCustomer.get().getId()).isNotEqualTo(foundSeller.get().getId());
    }

    @Test
    void 존재하지_않는_조합은_빈값을_반환한다() {
      Auth auth = Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      authRepository.save(auth);

      Optional<Auth> found = authRepository.findByEmailAndUserType("test@example.com", UserType.SELLER);

      assertThat(found).isEmpty();
    }
  }

  @Nested
  class 중복_확인_테스트 {

    @Test
    void 이메일_사용자유형_중복_존재_시_true를_반환한다() {
      Auth auth = Auth.register("test@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      authRepository.save(auth);

      boolean exists = authRepository.existsByEmailAndUserType("test@example.com", UserType.CUSTOMER);

      assertThat(exists).isTrue();
    }

    @Test
    void 이메일_사용자유형_중복_없으면_false를_반환한다() {
      boolean exists = authRepository.existsByEmailAndUserType("test@example.com", UserType.CUSTOMER);

      assertThat(exists).isFalse();
    }
  }

  @Nested
  class 소셜_로그인_조회_테스트 {

    @Test
    void 소셜_로그인_제공자_정보로_Auth를_조회한다() {
      Auth auth = Auth.registerWithOAuth(
          "test@example.com", "Pass123!", UserType.CUSTOMER,
          ProviderType.KAKAO, "kakao123", encoder, "SYSTEM");
      authRepository.save(auth);

      Optional<Auth> found = authRepository.findByProviderAndProviderUserId(ProviderType.KAKAO, "kakao123");

      assertThat(found).isPresent();
      assertThat(found.get().getEmail()).isEqualTo("test@example.com");
      assertThat(found.get().hasProvider(ProviderType.KAKAO)).isTrue();
    }

    @Test
    void 존재하지_않는_소셜_정보는_빈값을_반환한다() {
      Optional<Auth> found = authRepository.findByProviderAndProviderUserId(ProviderType.KAKAO, "nonexistent");

      assertThat(found).isEmpty();
    }
  }

  @Nested
  class 조건_검색_테스트 {

    @BeforeEach
    void setUpTestData() {
      Auth customer1 = Auth.register("customer1@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      Auth customer2 = Auth.register("customer2@example.com", "Pass123!", UserType.CUSTOMER, encoder, "SYSTEM");
      Auth seller = Auth.register("seller@example.com", "Pass123!", UserType.SELLER, encoder, "SYSTEM");
      Auth admin = Auth.register("admin@example.com", "Pass123!", UserType.ADMIN, encoder, "SYSTEM");

      customer1.connectProvider(ProviderType.KAKAO, "kakao1");
      customer2.connectProvider(ProviderType.NAVER, "naver1");
      customer2.lock();

      authRepository.save(customer1);
      authRepository.save(customer2);
      authRepository.save(seller);
      authRepository.save(admin);
    }

    @Test
    void 조건없이_전체를_조회한다() {
      AuthSearchCondition condition = AuthSearchCondition.builder().build();
      PageRequest pageable = PageRequest.of(0, 10);

      Page<Auth> result = authRepository.findAllByCondition(condition, pageable);

      assertThat(result.getTotalElements()).isEqualTo(4);
    }

    @Test
    void 이메일로_부분_검색한다() {
      AuthSearchCondition condition = AuthSearchCondition.builder()
          .email("customer")
          .build();
      PageRequest pageable = PageRequest.of(0, 10);

      Page<Auth> result = authRepository.findAllByCondition(condition, pageable);

      assertThat(result.getTotalElements()).isEqualTo(2);
      assertThat(result.getContent()).allMatch(auth -> auth.getEmail().contains("customer"));
    }

    @Test
    void 사용자_유형으로_검색한다() {
      AuthSearchCondition condition = AuthSearchCondition.builder()
          .userType(UserType.CUSTOMER)
          .build();
      PageRequest pageable = PageRequest.of(0, 10);

      Page<Auth> result = authRepository.findAllByCondition(condition, pageable);

      assertThat(result.getTotalElements()).isEqualTo(2);
      assertThat(result.getContent()).allMatch(auth -> auth.getUserType() == UserType.CUSTOMER);
    }

    @Test
    void 계정_상태로_검색한다() {
      AuthSearchCondition condition = AuthSearchCondition.builder()
          .status(AuthStatus.LOCKED)
          .build();
      PageRequest pageable = PageRequest.of(0, 10);

      Page<Auth> result = authRepository.findAllByCondition(condition, pageable);

      assertThat(result.getTotalElements()).isEqualTo(1);
      assertThat(result.getContent().get(0).getStatus()).isEqualTo(AuthStatus.LOCKED);
    }

    @Test
    void 소셜_로그인_제공자로_검색한다() {
      AuthSearchCondition condition = AuthSearchCondition.builder()
          .providerType(ProviderType.KAKAO)
          .build();
      PageRequest pageable = PageRequest.of(0, 10);

      Page<Auth> result = authRepository.findAllByCondition(condition, pageable);

      assertThat(result.getTotalElements()).isEqualTo(1);
      assertThat(result.getContent().get(0).hasProvider(ProviderType.KAKAO)).isTrue();
    }

    @Test
    void 복합_조건으로_검색한다() {
      AuthSearchCondition condition = AuthSearchCondition.builder()
          .userType(UserType.CUSTOMER)
          .status(AuthStatus.ACTIVE)
          .build();
      PageRequest pageable = PageRequest.of(0, 10);

      Page<Auth> result = authRepository.findAllByCondition(condition, pageable);

      assertThat(result.getTotalElements()).isEqualTo(1);
      assertThat(result.getContent().get(0).getEmail()).isEqualTo("customer1@example.com");
    }

    @Test
    void 정렬_조건을_적용한다() {
      AuthSearchCondition condition = AuthSearchCondition.builder().build();
      PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "email"));

      Page<Auth> result = authRepository.findAllByCondition(condition, pageable);

      assertThat(result.getContent().get(0).getEmail()).isEqualTo("admin@example.com");
    }

    @Test
    void 페이징을_적용한다() {
      AuthSearchCondition condition = AuthSearchCondition.builder().build();
      PageRequest pageable = PageRequest.of(0, 2);

      Page<Auth> result = authRepository.findAllByCondition(condition, pageable);

      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getTotalElements()).isEqualTo(4);
      assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void Provider_정보가_함께_조회된다() {
      AuthSearchCondition condition = AuthSearchCondition.builder()
          .providerType(ProviderType.KAKAO)
          .build();
      PageRequest pageable = PageRequest.of(0, 10);

      Page<Auth> result = authRepository.findAllByCondition(condition, pageable);

      assertThat(result.getContent().get(0).getProviders()).hasSize(1);
      assertThat(result.getContent().get(0).getProviders().get(0).getProvider())
          .isEqualTo(ProviderType.KAKAO);
    }
  }
}