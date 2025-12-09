package com.tickatch.user_service.global.config;

import io.github.tickatch.common.event.IntegrationEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

/**
 * Kafka Consumer 설정.
 *
 * <p>수동 커밋 및 에러 처리를 포함한 Consumer 설정을 제공한다.
 *
 * @author Tickatch
 * @since 1.0.0
 */
@EnableKafka
@Configuration
public class KafkaConsumerConfig {

  @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
  private String bootstrapServers;

  @Value("${spring.kafka.consumer.group-id:product-service}")
  private String groupId;

  @Bean
  public ConsumerFactory<String, IntegrationEvent> consumerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

    // 수동 커밋 설정
    configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

    // Deserializer 설정
    configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    configProps.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    configProps.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
    configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

    // JSON Deserializer 신뢰 패키지 설정
    configProps.put(
        JsonDeserializer.TRUSTED_PACKAGES, "io.github.tickatch.common.event,com.tickatch.*");

    return new DefaultKafkaConsumerFactory<>(configProps);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, IntegrationEvent>
      kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, IntegrationEvent> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());

    // 수동 커밋 모드
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

    // 동시 처리 스레드 수
    factory.setConcurrency(3);

    return factory;
  }
}
