package com.tickatch.auth_service.token.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tickatch.auth_service.token.domain.RefreshToken;
import com.tickatch.auth_service.token.domain.repository.RefreshTokenRepositoryImpl;
import com.tickatch.auth_service.token.domain.repository.dto.RefreshTokenSearchCondition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("RefreshTokenRepository 통합 테스트")
class RefreshTokenRepositoryTest {

  @Autowired
  private RefreshTokenRepositoryImpl refreshTokenRepository;

  private UUID authId;

  @BeforeEach
  void setUp() {
    authId = UUID.randomUUID();
  }

  @Nested
  class 저장_테스트 {

    @Test
    void RefreshToken을_저장하고_조회한다() {
      RefreshToken token = RefreshToken.create(authId, UUID.randomUUID().toString(), "Chrome/Windows", false);

      RefreshToken saved = refreshTokenRepository.save(token);

      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getAuthId()).isEqualTo(authId);
      assertThat(saved.getToken()).isNotNull();
    }

    @Test
    void ID로_RefreshToken을_조회한다() {
      RefreshToken token = RefreshToken.create(authId, UUID.randomUUID().toString(), "Chrome/Windows", false);
      RefreshToken saved = refreshTokenRepository.save(token);

      Optional<RefreshToken> found = refreshTokenRepository.findById(saved.getId());

      assertThat(found).isPresent();
      assertThat(found.get().getAuthId()).isEqualTo(authId);
    }
  }

  @Nested
  class 토큰값_조회_테스트 {

    @Test
    void 토큰_값으로_RefreshToken을_조회한다() {
      String tokenValue = UUID.randomUUID().toString();
      RefreshToken token = RefreshToken.create(authId, tokenValue, "Chrome/Windows", false);
      refreshTokenRepository.save(token);

      Optional<RefreshToken> found = refreshTokenRepository.findByToken(tokenValue);

      assertThat(found).isPresent();
      assertThat(found.get().getToken()).isEqualTo(tokenValue);
    }

    @Test
    void 존재하지_않는_토큰값은_빈값을_반환한다() {
      Optional<RefreshToken> found = refreshTokenRepository.findByToken("nonexistent-token");

      assertThat(found).isEmpty();
    }
  }

  @Nested
  class AuthId_조회_테스트 {

    @Test
    void AuthId로_모든_RefreshToken을_조회한다() {
      RefreshToken token1 = RefreshToken.create(authId, UUID.randomUUID().toString(), "Chrome/Windows", false);
      RefreshToken token2 = RefreshToken.create(authId, UUID.randomUUID().toString(), "Safari/Mac", true);
      RefreshToken otherToken = RefreshToken.create(UUID.randomUUID(), UUID.randomUUID().toString(), "Firefox/Linux", false);
      refreshTokenRepository.save(token1);
      refreshTokenRepository.save(token2);
      refreshTokenRepository.save(otherToken);

      List<RefreshToken> found = refreshTokenRepository.findAllByAuthId(authId);

      assertThat(found).hasSize(2);
      assertThat(found).allMatch(t -> t.getAuthId().equals(authId));
    }

    @Test
    void AuthId로_사용가능한_RefreshToken만_조회한다() {
      RefreshToken validToken = RefreshToken.create(authId, UUID.randomUUID().toString(), "Chrome/Windows", false);
      RefreshToken revokedToken = RefreshToken.create(authId, UUID.randomUUID().toString(), "Safari/Mac", false);
      revokedToken.revoke();
      refreshTokenRepository.save(validToken);
      refreshTokenRepository.save(revokedToken);

      List<RefreshToken> found = refreshTokenRepository.findAllUsableByAuthId(authId);

      assertThat(found).hasSize(1);
      assertThat(found.get(0).isRevoked()).isFalse();
    }
  }

  @Nested
  class 삭제_및_폐기_테스트 {

    @Test
    void AuthId로_모든_RefreshToken을_삭제한다() {
      RefreshToken token1 = RefreshToken.create(authId, UUID.randomUUID().toString(), "Chrome/Windows", false);
      RefreshToken token2 = RefreshToken.create(authId, UUID.randomUUID().toString(), "Safari/Mac", true);
      refreshTokenRepository.save(token1);
      refreshTokenRepository.save(token2);

      refreshTokenRepository.deleteAllByAuthId(authId);

      List<RefreshToken> found = refreshTokenRepository.findAllByAuthId(authId);
      assertThat(found).isEmpty();
    }

    @Test
    void AuthId로_모든_RefreshToken을_폐기한다() {
      RefreshToken token1 = RefreshToken.create(authId, UUID.randomUUID().toString(), "Chrome/Windows", false);
      RefreshToken token2 = RefreshToken.create(authId, UUID.randomUUID().toString(), "Safari/Mac", true);
      refreshTokenRepository.save(token1);
      refreshTokenRepository.save(token2);

      int revokedCount = refreshTokenRepository.revokeAllByAuthId(authId);

      assertThat(revokedCount).isEqualTo(2);
      List<RefreshToken> found = refreshTokenRepository.findAllByAuthId(authId);
      assertThat(found).allMatch(RefreshToken::isRevoked);
    }

    @Test
    void 이미_폐기된_토큰은_다시_폐기되지_않는다() {
      RefreshToken token = RefreshToken.create(authId, UUID.randomUUID().toString(), "Chrome/Windows", false);
      token.revoke();
      refreshTokenRepository.save(token);

      int revokedCount = refreshTokenRepository.revokeAllByAuthId(authId);

      assertThat(revokedCount).isZero();
    }
  }

  @Nested
  class 만료토큰_정리_테스트 {

    @Test
    void 폐기된_토큰을_삭제한다() {
      UUID authId1 = UUID.randomUUID();
      UUID authId2 = UUID.randomUUID();

      RefreshToken validToken = RefreshToken.create(authId1, UUID.randomUUID().toString(), "Chrome/Windows", false);
      refreshTokenRepository.save(validToken);

      RefreshToken revokedToken = RefreshToken.create(authId2, UUID.randomUUID().toString(), "Safari/Mac", false);
      revokedToken.revoke();
      refreshTokenRepository.save(revokedToken);

      int deletedCount = refreshTokenRepository.deleteExpiredAndRevokedTokens();

      assertThat(deletedCount).isEqualTo(1);
      assertThat(refreshTokenRepository.findById(validToken.getId())).isPresent();
      assertThat(refreshTokenRepository.findById(revokedToken.getId())).isEmpty();
    }
  }

  @Nested
  class 조건_검색_테스트 {

    private UUID authId1;
    private UUID authId2;

    @BeforeEach
    void setUpTestData() {
      authId1 = UUID.randomUUID();
      authId2 = UUID.randomUUID();

      RefreshToken token1 = RefreshToken.create(authId1, UUID.randomUUID().toString(), "Chrome/Windows", false);
      RefreshToken token2 = RefreshToken.create(authId1, UUID.randomUUID().toString(), "Safari/Mac", true);
      token2.revoke();

      RefreshToken token3 = RefreshToken.create(authId2, UUID.randomUUID().toString(), "Firefox/Linux", true);
      RefreshToken token4 = RefreshToken.create(authId2, UUID.randomUUID().toString(), "Edge/Windows", false);

      refreshTokenRepository.save(token1);
      refreshTokenRepository.save(token2);
      refreshTokenRepository.save(token3);
      refreshTokenRepository.save(token4);
    }

    @Test
    void 조건없이_전체를_조회한다() {
      RefreshTokenSearchCondition condition = RefreshTokenSearchCondition.builder().build();
      PageRequest pageable = PageRequest.of(0, 10);

      Page<RefreshToken> result = refreshTokenRepository.findAllByCondition(condition, pageable);

      assertThat(result.getTotalElements()).isEqualTo(4);
    }

    @Test
    void AuthId로_검색한다() {
      RefreshTokenSearchCondition condition = RefreshTokenSearchCondition.builder()
          .authId(authId1)
          .build();
      PageRequest pageable = PageRequest.of(0, 10);

      Page<RefreshToken> result = refreshTokenRepository.findAllByCondition(condition, pageable);

      assertThat(result.getTotalElements()).isEqualTo(2);
      assertThat(result.getContent()).allMatch(t -> t.getAuthId().equals(authId1));
    }

    @Test
    void 폐기여부로_검색한다() {
      RefreshTokenSearchCondition condition = RefreshTokenSearchCondition.builder()
          .revoked(true)
          .build();
      PageRequest pageable = PageRequest.of(0, 10);

      Page<RefreshToken> result = refreshTokenRepository.findAllByCondition(condition, pageable);

      assertThat(result.getTotalElements()).isEqualTo(1);
      assertThat(result.getContent().get(0).isRevoked()).isTrue();
    }

    @Test
    void 만료여부로_검색한다_유효한토큰() {
      RefreshTokenSearchCondition condition = RefreshTokenSearchCondition.builder()
          .expired(false)
          .build();
      PageRequest pageable = PageRequest.of(0, 10);

      Page<RefreshToken> result = refreshTokenRepository.findAllByCondition(condition, pageable);

      assertThat(result.getTotalElements()).isEqualTo(4);
    }

    @Test
    void 로그인유지_여부로_검색한다() {
      RefreshTokenSearchCondition condition = RefreshTokenSearchCondition.builder()
          .rememberMe(true)
          .build();
      PageRequest pageable = PageRequest.of(0, 10);

      Page<RefreshToken> result = refreshTokenRepository.findAllByCondition(condition, pageable);

      assertThat(result.getTotalElements()).isEqualTo(2);
      assertThat(result.getContent()).allMatch(RefreshToken::isRememberMe);
    }

    @Test
    void 디바이스정보로_검색한다() {
      RefreshTokenSearchCondition condition = RefreshTokenSearchCondition.builder()
          .deviceInfo("Windows")
          .build();
      PageRequest pageable = PageRequest.of(0, 10);

      Page<RefreshToken> result = refreshTokenRepository.findAllByCondition(condition, pageable);

      assertThat(result.getTotalElements()).isEqualTo(2);
      assertThat(result.getContent()).allMatch(t -> t.getDeviceInfo().contains("Windows"));
    }

    @Test
    void 복합_조건으로_검색한다() {
      RefreshTokenSearchCondition condition = RefreshTokenSearchCondition.builder()
          .authId(authId1)
          .revoked(false)
          .build();
      PageRequest pageable = PageRequest.of(0, 10);

      Page<RefreshToken> result = refreshTokenRepository.findAllByCondition(condition, pageable);

      assertThat(result.getTotalElements()).isEqualTo(1);
      assertThat(result.getContent().get(0).getDeviceInfo()).isEqualTo("Chrome/Windows");
    }

    @Test
    void 정렬_조건을_적용한다() {
      RefreshTokenSearchCondition condition = RefreshTokenSearchCondition.builder().build();
      PageRequest pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "deviceInfo"));

      Page<RefreshToken> result = refreshTokenRepository.findAllByCondition(condition, pageable);

      List<String> deviceInfos = result.getContent().stream()
          .map(RefreshToken::getDeviceInfo)
          .toList();
      assertThat(deviceInfos).isSorted();
    }

    @Test
    void 페이징을_적용한다() {
      RefreshTokenSearchCondition condition = RefreshTokenSearchCondition.builder().build();
      PageRequest pageable = PageRequest.of(0, 2);

      Page<RefreshToken> result = refreshTokenRepository.findAllByCondition(condition, pageable);

      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getTotalElements()).isEqualTo(4);
      assertThat(result.getTotalPages()).isEqualTo(2);
    }
  }
}