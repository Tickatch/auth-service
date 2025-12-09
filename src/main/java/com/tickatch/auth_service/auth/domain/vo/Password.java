package com.tickatch.auth_service.auth.domain.vo;

import com.tickatch.auth_service.auth.domain.exception.AuthErrorCode;
import com.tickatch.auth_service.auth.domain.exception.AuthException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 Value Object.
 *
 * <p>비밀번호 정책 검증 및 암호화된 값을 저장한다.
 *
 * <p>비밀번호 정책:
 *
 * <ul>
 *   <li>최소 8자 이상
 *   <li>영문, 숫자, 특수문자 3가지 모두 포함
 *   <li>허용 특수문자: ! @ # $ % ^ * ( ) _ + - = . , ?
 *   <li>보안상 위험한 문자 사용 불가: ' " < > & ; | ` $ \ { } [ ] :
 * </ul>
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Password {

  /* 최소 비밀번호 길이 */
  private static final int MIN_LENGTH = 8;
  /* 영문 패턴 */
  private static final Pattern LETTER_PATTERN = Pattern.compile("[a-zA-Z]");
  /* 숫자 패턴 */
  private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
  /* 허용된 특수문자 패턴 (안전한 문자만) */
  private static final Pattern SPECIAL_PATTERN = Pattern.compile("[!@#$%^*()_+\\-=.,?]");
  /* 허용된 전체 문자 패턴 (영문 + 숫자 + 허용 특수문자) */
  private static final Pattern ALLOWED_PATTERN = Pattern.compile("^[a-zA-Z0-9!@#$%^*()_+\\-=.,?]+$");

  /* 암호화된 비밀번호 */
  @Column(name = "password")
  private String encodedValue;

  private Password(String encodedValue) {
    this.encodedValue = encodedValue;
  }

  /**
   * 비밀번호를 검증 후 암호화하여 생성한다.
   *
   * @param rawPassword 원본 비밀번호
   * @param encoder 비밀번호 인코더
   * @return Password 객체
   * @throws AuthException 비밀번호 정책에 맞지 않을 경우
   */
  public static Password create(String rawPassword, PasswordEncoder encoder) {
    validatePolicy(rawPassword);
    return new Password(encoder.encode(rawPassword));
  }

  /**
   * 이미 암호화된 값으로 Password 객체를 생성한다.
   *
   * <p>DB에서 로드 시 사용한다.
   *
   * @param encodedValue 암호화된 비밀번호
   * @return Password 객체
   */
  public static Password fromEncoded(String encodedValue) {
    return new Password(encodedValue);
  }

  /**
   * 원본 비밀번호가 저장된 암호화 값과 일치하는지 확인한다.
   *
   * @param rawPassword 원본 비밀번호
   * @param encoder 비밀번호 인코더
   * @return 일치하면 true
   */
  public boolean matches(String rawPassword, PasswordEncoder encoder) {
    return encoder.matches(rawPassword, this.encodedValue);
  }

  /**
   * 비밀번호 정책을 검증한다.
   *
   * <p>검증 규칙:
   *
   * <ul>
   *   <li>최소 8자 이상
   *   <li>영문, 숫자, 특수문자 중 2가지 이상 조합
   * </ul>
   *
   * @param rawPassword 원본 비밀번호
   * @throws AuthException 정책에 맞지 않을 경우
   */
  public static void validatePolicy(String rawPassword) {
    if (rawPassword == null || rawPassword.length() < MIN_LENGTH) {
      throw new AuthException(AuthErrorCode.PASSWORD_TOO_SHORT);
    }

    int typeCount = 0;
    if (LETTER_PATTERN.matcher(rawPassword).find()) {
      typeCount++;
    }
    if (DIGIT_PATTERN.matcher(rawPassword).find()) {
      typeCount++;
    }
    if (SPECIAL_PATTERN.matcher(rawPassword).find()) {
      typeCount++;
    }

    if (typeCount < 2) {
      throw new AuthException(AuthErrorCode.PASSWORD_TOO_WEAK);
    }
  }

  /**
   * 암호화된 값이 존재하는지 확인한다.
   *
   * @return 값이 존재하면 true
   */
  public boolean hasValue() {
    return encodedValue != null && !encodedValue.isBlank();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Password password)) {
      return false;
    }
    return Objects.equals(encodedValue, password.encodedValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(encodedValue);
  }
}