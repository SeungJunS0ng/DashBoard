package com.festapp.dashboard.telemetry.service;

import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeDataService {

  private final SimpMessagingTemplate messagingTemplate;
  private final RedisTemplate<String, Object> redisTemplate;

  @RabbitListener(queues = "uemd.sensor.queue")
  public void processSensorData(SensorDataPayload payload) {

    String equipmentId = payload.getEquipmentId();

    // 1. Redis에 장비의 최신 상태(Snapshot) 업데이트
    String redisKey = "equipment:current:" + equipmentId;
    redisTemplate.opsForValue().set(redisKey, payload);

    // 2. 프론트엔드(WebSocket)로 실시간 데이터 브로드캐스팅
    // 프론트엔드는 /topic/equipment/CVD-CHAMBER-04 등을 구독
    messagingTemplate.convertAndSend("/topic/equipment/" + equipmentId, payload);

    log.debug("Data processed and broadcasted for equipment: {}", equipmentId);
  }
}