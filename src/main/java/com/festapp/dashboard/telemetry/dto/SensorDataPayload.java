package com.festapp.dashboard.telemetry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "현장 장비 및 시뮬레이터 실시간 데이터 페이로드")
public class SensorDataPayload {

  @Schema(description = "장비 식별자", example = "CVD-CHAMBER-04")
  private String equipmentId;

  @Schema(description = "데이터 발생 시간 (ISO 8601)", example = "2026-04-02T13:45:01.123Z")
  private String timestamp;

  @Schema(description = "장비 현재 상태", allowableValues = {"RUN", "STOP", "CRITICAL"}, example = "RUN")
  private String status;

  @Schema(description = "장비에 부착된 센서 데이터 리스트")
  private List<SensorDetails> sensors;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @Schema(description = "개별 센서 측정 상세 정보")
  public static class SensorDetails {

    @Schema(description = "센서 식별자", example = "Temp_0")
    private String sensorId;

    @Schema(description = "데이터 타입", allowableValues = {"FLOAT", "BOOLEAN", "INT"}, example = "FLOAT")
    private String dataType;

    @Schema(description = "센서 측정값 (숫자 혹은 논리값)", example = "971.5")
    private Object value;

    @Schema(description = "측정 단위", example = "°C")
    private String unit;
  }
}