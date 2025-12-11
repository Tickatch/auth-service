package com.tickatch.auth_service.global.jwt.presentation;

import com.tickatch.auth_service.token.application.port.out.TokenProvider;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JWKS (JSON Web Key Set) 제공 컨트롤러.
 *
 * <p>RFC 7517 표준에 따라 Public Key를 JWKS 형식으로 제공한다. Gateway나 다른 서비스에서 JWT 검증에 사용한다.
 *
 * <p>Public Key만 노출하므로 외부에 공개되어도 안전하다.
 *
 * @author Tickatch
 * @since 1.0.0
 * @see <a href="https://tools.ietf.org/html/rfc7517">RFC 7517 - JSON Web Key</a>
 */
@RestController
@RequiredArgsConstructor
public class JwtKeyController {

  private final TokenProvider tokenProvider;

  /**
   * JWKS (JSON Web Key Set)를 반환한다.
   *
   * <p>표준 경로: /.well-known/jwks.json
   *
   * <p>응답 예시:
   * <pre>
   * {
   *   "keys": [{
   *     "kty": "RSA",
   *     "alg": "RS256",
   *     "use": "sig",
   *     "kid": "tickatch-auth-key-1",
   *     "n": "...",
   *     "e": "AQAB"
   *   }]
   * }
   * </pre>
   *
   * @return JWKS 응답
   */
  @GetMapping("/.well-known/jwks.json")
  public ResponseEntity<Map<String, Object>> getJwks() {
    RSAPublicKey publicKey = tokenProvider.getPublicKey();

    Map<String, Object> jwk = Map.of(
        "kty", "RSA",
        "alg", "RS256",
        "use", "sig",
        "kid", tokenProvider.getKeyId(),
        "n", base64UrlEncode(publicKey.getModulus().toByteArray()),
        "e", base64UrlEncode(publicKey.getPublicExponent().toByteArray())
    );

    Map<String, Object> jwks = Map.of("keys", List.of(jwk));

    return ResponseEntity.ok(jwks);
  }

  /**
   * Public Key를 PEM 형식으로 반환한다.
   *
   * <p>JWKS를 파싱하기 어려운 환경에서 사용할 수 있다.
   *
   * @return PEM 형식의 Public Key
   */
  @GetMapping("/.well-known/public-key.pem")
  public ResponseEntity<String> getPublicKeyPem() {
    RSAPublicKey publicKey = tokenProvider.getPublicKey();
    String pem = toPemFormat(publicKey.getEncoded());

    return ResponseEntity.ok(pem);
  }

  /**
   * 바이트 배열을 Base64 URL 인코딩한다.
   *
   * <p>JWKS 표준에서는 Base64 URL 인코딩을 사용한다 (패딩 없음).
   */
  private String base64UrlEncode(byte[] bytes) {
    // BigInteger는 앞에 부호 비트가 붙을 수 있으므로 제거
    if (bytes.length > 0 && bytes[0] == 0) {
      byte[] trimmed = new byte[bytes.length - 1];
      System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
      bytes = trimmed;
    }
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /**
   * 바이트 배열을 PEM 형식으로 변환한다.
   */
  private String toPemFormat(byte[] keyBytes) {
    String base64 = Base64.getEncoder().encodeToString(keyBytes);
    StringBuilder pem = new StringBuilder();

    pem.append("-----BEGIN PUBLIC KEY-----\n");
    for (int i = 0; i < base64.length(); i += 64) {
      pem.append(base64, i, Math.min(i + 64, base64.length()));
      pem.append("\n");
    }
    pem.append("-----END PUBLIC KEY-----\n");

    return pem.toString();
  }
}