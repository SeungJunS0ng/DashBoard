// 위젯 응답 DTO - 위젯 정보를 클라이언트에 반환
package com.festapp.dashboard.dashboard.widget.dto;

import com.festapp.dashboard.dashboard.widget.entity.DashboardWidget;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 위젯 응답 DTO
 *
 * 위젯 정보를 클라이언트에 반환할 때 사용합니다.
 * 모든 필드는 서버에서 생성되며, 클라이언트는 이를 읽기만 합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "위젯 응답 DTO")
public class WidgetResponseDto {

    @Schema(description = "위젯 고유 ID (자동 생성)", example = "1")
    private Long id;

    @Schema(description = "위젯 소유자의 사용자 ID", example = "100")
    private Long userId;

    @Schema(description = "장비 ID", example = "CVD-CHAMBER-01")
    private String equipmentId;

    @Schema(description = "위젯 타입", example = "GAUGE")
    private String widgetType;

    @Schema(description = "위젯 제목", example = "Temperature Gauge")
    private String title;

    @Schema(description = "센서 ID", example = "Temp_Sensor_001")
    private String sensorId;

    @Schema(description = "차트 타입", example = "line")
    private String chartType;

    @Schema(description = "데이터 타입", example = "FLOAT")
    private String dataType;

    @Schema(description = "단위", example = "°C")
    private String unit;

    @Schema(description = "X 위치 (그리드 좌표)", example = "0")
    private Integer posX;

    @Schema(description = "Y 위치 (그리드 좌표)", example = "0")
    private Integer posY;

    @Schema(description = "너비 (그리드 단위)", example = "4")
    private Integer width;

    @Schema(description = "높이 (그리드 단위)", example = "3")
    private Integer height;

    @Schema(description = "추가 설정 정보 (JSON 형식)", example = "{\"refreshInterval\":5000,\"decimals\":2}")
    private String configJson;

    @Schema(description = "위젯 생성 시간", example = "2026-04-03T10:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "위젯 마지막 수정 시간", example = "2026-04-03T11:45:30")
    private LocalDateTime updatedAt;

    public static WidgetResponseDto fromEntity(DashboardWidget widget) {
        return WidgetResponseDto.builder()
                .id(widget.getId())
                .userId(widget.getUser().getUserId())
                .equipmentId(widget.getEquipmentId())
                .widgetType(widget.getWidgetType())
                .title(widget.getTitle())
                .sensorId(widget.getSensorId())
                .chartType(widget.getChartType())
                .dataType(widget.getDataType())
                .unit(widget.getUnit())
                .posX(widget.getPosX())
                .posY(widget.getPosY())
                .width(widget.getWidth())
                .height(widget.getHeight())
                .configJson(widget.getConfigJson())
                .createdAt(widget.getCreatedAt())
                .updatedAt(widget.getUpdatedAt())
                .build();
    }
}

