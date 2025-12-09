package com.tickatch.user_service.global.config;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 처리 설정.
 *
 * <p>상품 서비스의 비동기 작업(이미지 처리, 이벤트 발행 등)을 위한 스레드 풀을 구성한다.
 *
 * <p>스레드 풀 구성:
 *
 * <ul>
 *   <li>코어 스레드: 5개
 *   <li>최대 스레드: 10개
 *   <li>대기 큐 용량: 50개
 *   <li>스레드 이름 접두사: product-async-
 * </ul>
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(5);
    executor.setMaxPoolSize(10);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("product-async-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.setRejectedExecutionHandler(
        (r, e) -> log.warn("Task rejected, thread pool is full and queue is full"));
    executor.initialize();
    return executor;
  }

  /** 이벤트 발행 전용 스레드 풀. 도메인 이벤트를 Kafka로 발행할 때 사용한다. */
  @Bean(name = "eventExecutor")
  public Executor eventExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(3);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("product-event-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.initialize();
    return executor;
  }

  @Override
  public Executor getAsyncExecutor() {
    return taskExecutor();
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return new AsyncExceptionHandler();
  }

  /** 비동기 작업 예외 핸들러. */
  @Slf4j
  static class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
      log.error(
          "비동기 작업 예외 발생 - method: {}.{}, params: {}",
          method.getDeclaringClass().getSimpleName(),
          method.getName(),
          params,
          ex);
    }
  }
}
