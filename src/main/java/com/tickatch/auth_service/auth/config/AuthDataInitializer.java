package com.tickatch.auth_service.config;

import com.tickatch.auth_service.auth.domain.Auth;
import com.tickatch.auth_service.auth.domain.repository.AuthJpaRepository;
import com.tickatch.auth_service.auth.domain.vo.UserType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Auth 초기 데이터 설정.
 *
 * <p>애플리케이션 시작 시 기본 계정(구매자/판매자/관리자)을 생성한다.
 * 개발 및 로컬 환경에서만 동작하며, 이미 존재하는 계정은 생성하지 않는다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Slf4j
@Component
//@Profile({"local", "dev"})  // 운영 환경에서는 비활성화
@RequiredArgsConstructor
public class AuthDataInitializer implements ApplicationRunner {

  private final AuthJpaRepository authJpaRepository;
  private final PasswordEncoder passwordEncoder;

  // 기본 비밀번호 (영문 + 숫자 + 특수문자 조합)
  private static final String DEFAULT_PASSWORD = "Test1234!";
  private static final String SYSTEM_USER = "SYSTEM";

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    log.info("========== Auth 초기 데이터 설정 시작 ==========");

    createDefaultCustomer();
    createDefaultSeller();
    createDefaultAdmin();

    log.info("========== Auth 초기 데이터 설정 완료 ==========");
  }

  /**
   * 기본 구매자 계정을 생성한다.
   */
  private void createDefaultCustomer() {
    String email = "customer@tickatch.com";
    UserType userType = UserType.CUSTOMER;

    if (authJpaRepository.existsByEmailAndUserType(email, userType)) {
      log.info("[SKIP] 구매자 계정이 이미 존재합니다: {}", email);
      return;
    }

    Auth customer = Auth.register(
        email,
        DEFAULT_PASSWORD,
        userType,
        passwordEncoder,
        SYSTEM_USER
    );

    authJpaRepository.save(customer);
    log.info("[CREATE] 구매자 계정 생성 완료: {} / {}", email, DEFAULT_PASSWORD);
  }

  /**
   * 기본 판매자 계정을 생성한다.
   */
  private void createDefaultSeller() {
    String email = "seller@tickatch.com";
    UserType userType = UserType.SELLER;

    if (authJpaRepository.existsByEmailAndUserType(email, userType)) {
      log.info("[SKIP] 판매자 계정이 이미 존재합니다: {}", email);
      return;
    }

    Auth seller = Auth.register(
        email,
        DEFAULT_PASSWORD,
        userType,
        passwordEncoder,
        SYSTEM_USER
    );

    authJpaRepository.save(seller);
    log.info("[CREATE] 판매자 계정 생성 완료: {} / {}", email, DEFAULT_PASSWORD);
  }

  /**
   * 기본 관리자 계정을 생성한다.
   */
  private void createDefaultAdmin() {
    String email = "admin@tickatch.com";
    UserType userType = UserType.ADMIN;

    if (authJpaRepository.existsByEmailAndUserType(email, userType)) {
      log.info("[SKIP] 관리자 계정이 이미 존재합니다: {}", email);
      return;
    }

    Auth admin = Auth.register(
        email,
        DEFAULT_PASSWORD,
        userType,
        passwordEncoder,
        SYSTEM_USER
    );

    authJpaRepository.save(admin);
    log.info("[CREATE] 관리자 계정 생성 완료: {} / {}", email, DEFAULT_PASSWORD);
  }
}