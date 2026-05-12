package com.festapp.dashboard.telemetry.service;

import com.festapp.dashboard.telemetry.dto.SensorDataPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final SensorHistoryService sensorHistoryService;

  public void processSensorData(SensorDataPayload payload) {
    if (payload == null) {
      log.warn("Skipping telemetry processing because payload is null");
      return;
    }

    String equipmentId = payload.getEquipmentId();
    Long equipmentEntityId = payload.getEquipmentEntityId();
    if (equipmentEntityId == null && (equipmentId == null || equipmentId.isBlank())) {
      log.warn("Skipping telemetry processing because equipment reference is missing");
      return;
    }

    autoTagging(payload.getSensors());

    sensorHistoryService.persistTelemetryPayload(payload);

    try {
      updateCurrentSnapshot(payload);
    } catch (Exception e) {
      log.warn("Redis snapshot update skipped for equipment {}: {}", resolveLogEquipment(payload), e.getMessage());
    }

    // 2. 프론트엔드(WebSocket)로 실시간 데이터 브로드캐스팅
    // 프론트엔드는 /topic/equipment/CVD-CHAMBER-04 등을 구독
    if (equipmentId != null && !equipmentId.isBlank()) {
      messagingTemplate.convertAndSend("/topic/equipment/" + equipmentId, payload);
    }
    if (equipmentEntityId != null) {
      messagingTemplate.convertAndSend("/topic/equipment-id/" + equipmentEntityId, payload);
    }

    log.debug("Data processed and broadcasted for equipment: {}", resolveLogEquipment(payload));
  }

  private void updateCurrentSnapshot(SensorDataPayload payload) {
    if (payload.getEquipmentEntityId() != null) {
      redisTemplate.opsForValue().set("equipment:current:id:" + payload.getEquipmentEntityId(), payload);
    }
    if (payload.getEquipmentId() != null && !payload.getEquipmentId().isBlank()) {
      redisTemplate.opsForValue().set("equipment:current:" + payload.getEquipmentId(), payload);
    }
  }

  private String resolveLogEquipment(SensorDataPayload payload) {
    if (payload.getEquipmentEntityId() != null) {
      return "id:" + payload.getEquipmentEntityId();
    }
    return payload.getEquipmentId();
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
