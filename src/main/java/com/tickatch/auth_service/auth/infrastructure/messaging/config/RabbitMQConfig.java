package com.tickatch.auth_service.auth.infrastructure.messaging.config;

import io.github.tickatch.common.util.JsonUtils;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auth Service RabbitMQ 설정.
 *
 * <p>User Service에서 발행하는 이벤트를 수신하고, 로그 서비스로 로그를 발행하기 위한 Exchange, Queue, Binding을 정의한다.
 *
 * <p>수신하는 이벤트:
 *
 * <ul>
 *   <li>사용자 탈퇴 (customer.withdrawn, seller.withdrawn, admin.withdrawn)
 *   <li>사용자 정지 (customer.suspended, seller.suspended, admin.suspended)
 *   <li>사용자 활성화 (customer.activated, seller.activated, admin.activated)
 * </ul>
 *
 * <p>발행하는 이벤트:
 *
 * <ul>
 *   <li>인증 로그 (auth.log) → 로그 서비스
 * </ul>
 *
 * @author Tickatch
 * @since 1.0.0
 */
@Configuration
public class RabbitMQConfig {

  @Value("${messaging.exchange.user:tickatch.user}")
  private String userExchange;

  @Value("${messaging.exchange.log:tickatch.log}")
  private String logExchange;

  // ========================================
  // Queue Names - User Service 이벤트 수신용
  // ========================================

  /** 탈퇴 이벤트 수신 큐 */
  public static final String QUEUE_USER_WITHDRAWN_AUTH = "tickatch.user.withdrawn.auth.queue";

  /** 정지 이벤트 수신 큐 */
  public static final String QUEUE_USER_SUSPENDED_AUTH = "tickatch.user.suspended.auth.queue";

  /** 활성화 이벤트 수신 큐 */
  public static final String QUEUE_USER_ACTIVATED_AUTH = "tickatch.user.activated.auth.queue";

  // ========================================
  // Routing Keys - User Service 이벤트 수신용
  // ========================================

  public static final String ROUTING_KEY_CUSTOMER_WITHDRAWN = "customer.withdrawn";
  public static final String ROUTING_KEY_CUSTOMER_SUSPENDED = "customer.suspended";
  public static final String ROUTING_KEY_CUSTOMER_ACTIVATED = "customer.activated";

  public static final String ROUTING_KEY_SELLER_WITHDRAWN = "seller.withdrawn";
  public static final String ROUTING_KEY_SELLER_SUSPENDED = "seller.suspended";
  public static final String ROUTING_KEY_SELLER_ACTIVATED = "seller.activated";

  public static final String ROUTING_KEY_ADMIN_WITHDRAWN = "admin.withdrawn";
  public static final String ROUTING_KEY_ADMIN_SUSPENDED = "admin.suspended";
  public static final String ROUTING_KEY_ADMIN_ACTIVATED = "admin.activated";

  // ========================================
  // Routing Keys - 로그 발행용
  // ========================================

  /** 인증 로그 라우팅 키 */
  public static final String ROUTING_KEY_AUTH_LOG = "auth.log";

  // ========================================
  // Queue Names - 로그 발행용
  // ========================================

  /** 인증 로그 큐 이름 */
  public static final String QUEUE_AUTH_LOG = "tickatch.auth.log.queue";

  // ========================================
  // Exchange - User Service 이벤트 수신용
  // ========================================

  @Bean
  public TopicExchange userExchange() {
    return ExchangeBuilder.topicExchange(userExchange).durable(true).build();
  }

  // ========================================
  // Exchange - 로그 발행용
  // ========================================

  /**
   * 로그 이벤트용 Topic Exchange를 생성한다.
   *
   * @return durable Topic Exchange
   */
  @Bean
  public TopicExchange logExchange() {
    return ExchangeBuilder.topicExchange(logExchange).durable(true).build();
  }

  // ========================================
  // Queues - User Service 이벤트 수신용
  // ========================================

  @Bean
  public Queue userWithdrawnAuthQueue() {
    return QueueBuilder.durable(QUEUE_USER_WITHDRAWN_AUTH)
        .withArgument("x-dead-letter-exchange", userExchange + ".dlx")
        .withArgument("x-dead-letter-routing-key", "dlq.withdrawn.auth")
        .build();
  }

  @Bean
  public Queue userSuspendedAuthQueue() {
    return QueueBuilder.durable(QUEUE_USER_SUSPENDED_AUTH)
        .withArgument("x-dead-letter-exchange", userExchange + ".dlx")
        .withArgument("x-dead-letter-routing-key", "dlq.suspended.auth")
        .build();
  }

  @Bean
  public Queue userActivatedAuthQueue() {
    return QueueBuilder.durable(QUEUE_USER_ACTIVATED_AUTH)
        .withArgument("x-dead-letter-exchange", userExchange + ".dlx")
        .withArgument("x-dead-letter-routing-key", "dlq.activated.auth")
        .build();
  }

  // ========================================
  // Queues - 로그 발행용
  // ========================================

  /**
   * 인증 로그 큐를 생성한다.
   *
   * @return DLQ 설정이 포함된 durable Queue
   */
  @Bean
  public Queue authLogQueue() {
    return QueueBuilder.durable(QUEUE_AUTH_LOG)
        .withArgument("x-dead-letter-exchange", logExchange + ".dlx")
        .withArgument("x-dead-letter-routing-key", "dlq." + ROUTING_KEY_AUTH_LOG)
        .build();
  }

  // ========================================
  // Bindings - 탈퇴 이벤트
  // ========================================

  @Bean
  public Binding customerWithdrawnAuthBinding(
      Queue userWithdrawnAuthQueue, TopicExchange userExchange) {
    return BindingBuilder.bind(userWithdrawnAuthQueue)
        .to(userExchange)
        .with(ROUTING_KEY_CUSTOMER_WITHDRAWN);
  }

  @Bean
  public Binding sellerWithdrawnAuthBinding(
      Queue userWithdrawnAuthQueue, TopicExchange userExchange) {
    return BindingBuilder.bind(userWithdrawnAuthQueue)
        .to(userExchange)
        .with(ROUTING_KEY_SELLER_WITHDRAWN);
  }

  @Bean
  public Binding adminWithdrawnAuthBinding(
      Queue userWithdrawnAuthQueue, TopicExchange userExchange) {
    return BindingBuilder.bind(userWithdrawnAuthQueue)
        .to(userExchange)
        .with(ROUTING_KEY_ADMIN_WITHDRAWN);
  }

  // ========================================
  // Bindings - 정지 이벤트
  // ========================================

  @Bean
  public Binding customerSuspendedAuthBinding(
      Queue userSuspendedAuthQueue, TopicExchange userExchange) {
    return BindingBuilder.bind(userSuspendedAuthQueue)
        .to(userExchange)
        .with(ROUTING_KEY_CUSTOMER_SUSPENDED);
  }

  @Bean
  public Binding sellerSuspendedAuthBinding(
      Queue userSuspendedAuthQueue, TopicExchange userExchange) {
    return BindingBuilder.bind(userSuspendedAuthQueue)
        .to(userExchange)
        .with(ROUTING_KEY_SELLER_SUSPENDED);
  }

  @Bean
  public Binding adminSuspendedAuthBinding(
      Queue userSuspendedAuthQueue, TopicExchange userExchange) {
    return BindingBuilder.bind(userSuspendedAuthQueue)
        .to(userExchange)
        .with(ROUTING_KEY_ADMIN_SUSPENDED);
  }

  // ========================================
  // Bindings - 활성화 이벤트
  // ========================================

  @Bean
  public Binding customerActivatedAuthBinding(
      Queue userActivatedAuthQueue, TopicExchange userExchange) {
    return BindingBuilder.bind(userActivatedAuthQueue)
        .to(userExchange)
        .with(ROUTING_KEY_CUSTOMER_ACTIVATED);
  }

  @Bean
  public Binding sellerActivatedAuthBinding(
      Queue userActivatedAuthQueue, TopicExchange userExchange) {
    return BindingBuilder.bind(userActivatedAuthQueue)
        .to(userExchange)
        .with(ROUTING_KEY_SELLER_ACTIVATED);
  }

  @Bean
  public Binding adminActivatedAuthBinding(
      Queue userActivatedAuthQueue, TopicExchange userExchange) {
    return BindingBuilder.bind(userActivatedAuthQueue)
        .to(userExchange)
        .with(ROUTING_KEY_ADMIN_ACTIVATED);
  }

  // ========================================
  // Bindings - 로그 이벤트
  // ========================================

  /**
   * 인증 로그 큐와 로그 Exchange를 바인딩한다.
   *
   * @param authLogQueue 바인딩할 큐
   * @param logExchange 바인딩할 Exchange
   * @return 라우팅 키로 연결된 Binding
   */
  @Bean
  public Binding authLogBinding(Queue authLogQueue, TopicExchange logExchange) {
    return BindingBuilder.bind(authLogQueue).to(logExchange).with(ROUTING_KEY_AUTH_LOG);
  }

  // ========================================
  // Dead Letter Exchange & Queues - User Service 이벤트용
  // ========================================

  @Bean
  public TopicExchange userDeadLetterExchange() {
    return ExchangeBuilder.topicExchange(userExchange + ".dlx").durable(true).build();
  }

  @Bean
  public Queue userWithdrawnAuthDlq() {
    return QueueBuilder.durable(QUEUE_USER_WITHDRAWN_AUTH + ".dlq").build();
  }

  @Bean
  public Queue userSuspendedAuthDlq() {
    return QueueBuilder.durable(QUEUE_USER_SUSPENDED_AUTH + ".dlq").build();
  }

  @Bean
  public Queue userActivatedAuthDlq() {
    return QueueBuilder.durable(QUEUE_USER_ACTIVATED_AUTH + ".dlq").build();
  }

  @Bean
  public Binding userWithdrawnAuthDlqBinding(
      Queue userWithdrawnAuthDlq, TopicExchange userDeadLetterExchange) {
    return BindingBuilder.bind(userWithdrawnAuthDlq)
        .to(userDeadLetterExchange)
        .with("dlq.withdrawn.auth");
  }

  @Bean
  public Binding userSuspendedAuthDlqBinding(
      Queue userSuspendedAuthDlq, TopicExchange userDeadLetterExchange) {
    return BindingBuilder.bind(userSuspendedAuthDlq)
        .to(userDeadLetterExchange)
        .with("dlq.suspended.auth");
  }

  @Bean
  public Binding userActivatedAuthDlqBinding(
      Queue userActivatedAuthDlq, TopicExchange userDeadLetterExchange) {
    return BindingBuilder.bind(userActivatedAuthDlq)
        .to(userDeadLetterExchange)
        .with("dlq.activated.auth");
  }

  // ========================================
  // Dead Letter Exchange & Queues - 로그 이벤트용
  // ========================================

  /**
   * 로그 Dead Letter Exchange를 생성한다.
   *
   * @return DLX용 Topic Exchange
   */
  @Bean
  public TopicExchange logDeadLetterExchange() {
    return ExchangeBuilder.topicExchange(logExchange + ".dlx").durable(true).build();
  }

  /**
   * 인증 로그 Dead Letter Queue를 생성한다.
   *
   * @return durable DLQ
   */
  @Bean
  public Queue authLogDlq() {
    return QueueBuilder.durable(QUEUE_AUTH_LOG + ".dlq").build();
  }

  /**
   * 인증 로그 DLQ와 로그 DLX를 바인딩한다.
   *
   * @param authLogDlq 바인딩할 DLQ
   * @param logDeadLetterExchange 바인딩할 DLX
   * @return DLQ Binding
   */
  @Bean
  public Binding authLogDlqBinding(Queue authLogDlq, TopicExchange logDeadLetterExchange) {
    return BindingBuilder.bind(authLogDlq)
        .to(logDeadLetterExchange)
        .with("dlq." + ROUTING_KEY_AUTH_LOG);
  }

  // ========================================
  // Message Converter & Template
  // ========================================

  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter(JsonUtils.getObjectMapper());
  }

  @Bean
  public RabbitTemplate rabbitTemplate(
      ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
    RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
    rabbitTemplate.setMessageConverter(jsonMessageConverter);
    return rabbitTemplate;
  }
}
