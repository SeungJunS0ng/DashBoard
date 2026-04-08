package com.festapp.dashboard.telemetry.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

  public static final String SENSOR_QUEUE = "uemd.sensor.queue";

  // 1. 센서 데이터를 받을 큐 생성
  @Bean
  public Queue sensorQueue() {
    return new Queue(SENSOR_QUEUE, true);
  }

  // 2. JSON 형태의 메시지를 Java 객체(SensorDataPayload)로 자동 변환
  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }
}