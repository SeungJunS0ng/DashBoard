// 위젯 생성/수정 요청 DTO - 클라이언트로부터 받은 위젯 정보
package com.festapp.dashboard.dashboard.widget.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 위젯 생성/수정 요청 DTO
 *
 * 프론트엔드에서 위젯을 생성하거나 수정할 때 사용합니다.
 * 모든 필드의 유효성은 서버에서 검증되며, 검증 실패 시 400 Bad Request를 반환합니다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Schema(description = "위젯 생성/수정 요청 DTO")
public class WidgetRequestDto {

    @NotBlank(message = "Equipment ID는 필수입니다")
    @Size(max = 255, message = "Equipment ID는 255자 이하여야 합니다")
    @Schema(description = "장비 ID (필수, 255자 이하)", example = "CVD-CHAMBER-01")
    private String equipmentId;

    @NotBlank(message = "Widget type은 필수입니다")
    @Size(max = 50, message = "Widget type은 50자 이하여야 합니다")
    @Schema(description = "위젯 타입 (필수, 50자 이하) - 예: GAUGE, CHART, TABLE 등", example = "GAUGE")
    private String widgetType;

    @NotBlank(message = "Title은 필수입니다")
    @Size(max = 255, message = "Title은 255자 이하여야 합니다")
    @Schema(description = "위젯 제목 (필수, 255자 이하)", example = "Temperature Gauge")
    private String title;

    @Size(max = 255, message = "Sensor ID는 255자 이하여야 합니다")
    @Schema(description = "센서 ID (선택사항, 255자 이하) - 이 위젯이 표시할 센서의 ID", example = "Temp_Sensor_001")
    private String sensorId;

    @Size(max = 50, message = "Chart type은 50자 이하여야 합니다")
    @Schema(description = "차트 타입 (선택사항, 50자 이하) - 예: line, bar, pie 등", example = "line")
    private String chartType;

    @Size(max = 50, message = "Data type은 50자 이하여야 합니다")
    @Schema(description = "데이터 타입 (선택사항, 50자 이하) - 예: FLOAT, INTEGER, STRING 등", example = "FLOAT")
    private String dataType;

    @Size(max = 50, message = "Unit은 50자 이하여야 합니다")
    @Schema(description = "단위 (선택사항, 50자 이하) - 예: °C, %, V 등", example = "°C")
    private String unit;

    @NotNull(message = "Position X는 필수입니다")
    @Min(value = 0, message = "Position X는 0 이상이어야 합니다")
    @Schema(description = "X 위치 (필수, 0 이상) - 그리드의 X 좌표", example = "0")
    private Integer posX;

    @NotNull(message = "Position Y는 필수입니다")
    @Min(value = 0, message = "Position Y는 0 이상이어야 합니다")
    @Schema(description = "Y 위치 (필수, 0 이상) - 그리드의 Y 좌표", example = "0")
    private Integer posY;

    @NotNull(message = "Width는 필수입니다")
    @Min(value = 1, message = "Width는 1 이상이어야 합니다")
    @Schema(description = "너비 (필수, 1 이상) - 그리드 단위의 너비", example = "4")
    private Integer width;

    @NotNull(message = "Height는 필수입니다")
    @Min(value = 1, message = "Height는 1 이상이어야 합니다")
    @Schema(description = "높이 (필수, 1 이상) - 그리드 단위의 높이", example = "3")
    private Integer height;

    @Schema(description = "추가 설정 정보 (선택사항) - JSON 형식의 위젯별 커스텀 설정", example = "{\"refreshInterval\":5000,\"decimals\":2}")
    private String configJson;
}

