package com.tickatch.auth_service.global.config;

import io.github.tickatch.common.security.BaseSecurityConfig;
import io.github.tickatch.common.security.LoginFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends BaseSecurityConfig {

  @Bean
  @Override
  protected LoginFilter loginFilterBean() {
    return new LoginFilter();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return build(http);
  }

  /**
   * 비밀번호 인코더 빈.
   *
   * <p>BCrypt 알고리즘을 사용하여 비밀번호를 안전하게 해싱한다.
   *
   * @return PasswordEncoder
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Override
  protected Customizer<
          AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry>
      authorizeHttpRequests() {
    return registry ->
        registry
            // 기본 허용 경로 (Swagger, Actuator)
            .requestMatchers(defaultPermitAllPaths())
            .permitAll()
            // 상품 조회 API는 인증 없이 허용
            .requestMatchers(HttpMethod.GET, "/api/v1/products/**")
            .permitAll()
            // 나머지는 인증 필요
            .anyRequest()
            .authenticated();
  }
}
