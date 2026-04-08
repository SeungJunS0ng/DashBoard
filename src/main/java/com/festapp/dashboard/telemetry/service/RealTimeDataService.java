package com.festapp.dashboard.telemetry.service;

import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeDataService {

  private final SimpMessagingTemplate messagingTemplate;
  private final RedisTemplate<String, Object> redisTemplate;

  @RabbitListener(queues = "uemd.sensor.queue")
  public void processSensorData(SensorDataPayload payload) {

    autoTagging(payload.getSensors());

    String equipmentId = payload.getEquipmentId();

    // 1. Redis에 장비의 최신 상태(Snapshot) 업데이트
    String redisKey = "equipment:current:" + equipmentId;
    redisTemplate.opsForValue().set(redisKey, payload);

    // 2. 프론트엔드(WebSocket)로 실시간 데이터 브로드캐스팅
    // 프론트엔드는 /topic/equipment/CVD-CHAMBER-04 등을 구독
    messagingTemplate.convertAndSend("/topic/equipment/" + equipmentId, payload);

    log.debug("Data processed and broadcasted for equipment: {}", equipmentId);
  }

  private void autoTagging(List<SensorDataPayload.SensorDetails> sensors) {
    if (sensors == null) return;

    for (SensorDataPayload.SensorDetails sensor : sensors) {
      Object value = sensor.getValue();
      String unit = (sensor.getUnit() != null) ? sensor.getUnit().toUpperCase() : "";

      // 1. Boolean 판별 (논리형이거나 유닛이 BOOL/STATUS 인 경우)
      if (value instanceof Boolean || unit.contains("BOOL") || unit.contains("STATUS")) {
        sensor.setDataType("BOOLEAN");
        // 정수형(0, 1)으로 들어온 경우를 대비해 Boolean으로 형변환 로직을 추가할 수도 있습니다.
      }
      // 2. 숫자형 판별
      else if (value instanceof Number) {
        double doubleVal = ((Number) value).doubleValue();

        // 소수점 이하 자리가 있는지 확인
        if (doubleVal % 1 != 0) {
          sensor.setDataType("FLOAT");
        } else {
          sensor.setDataType("INTEGER");
        }
      }
      // 3. 그 외 문자열
      else {
        sensor.setDataType("STRING");
      }
    }
  }
}