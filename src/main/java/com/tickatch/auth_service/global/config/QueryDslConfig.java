package com.tickatch.user_service.global.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * QueryDSL 설정.
 *
 * <p>JPAQueryFactory 빈을 등록하여 타입 안전한 쿼리 작성을 지원한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Configuration
public class QueryDslConfig {

  @PersistenceContext private EntityManager entityManager;

  @Bean
  public JPAQueryFactory jpaQueryFactory() {
    return new JPAQueryFactory(entityManager);
  }
}
