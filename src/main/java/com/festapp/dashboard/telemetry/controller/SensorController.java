package com.festapp.dashboard.telemetry.controller;

import com.festapp.dashboard.common.dto.ApiResponse;
import com.festapp.dashboard.common.security.SecurityContextHelper;
import com.festapp.dashboard.telemetry.dto.SensorRequest;
import com.festapp.dashboard.telemetry.dto.SensorResponse;
import com.festapp.dashboard.telemetry.service.SensorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sensors")
@RequiredArgsConstructor
@Tag(name = "Sensors", description = "장비 아래에 속한 센서를 관리하는 API입니다. 프론트에서는 장비 상세 화면이나 센서 설정 화면에서 사용합니다.")
@SecurityRequirement(name = "bearer-jwt")
public class SensorController {

    private final SensorService sensorService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "센서 생성", description = "프론트 사용 시점: 장비 상세 화면에서 센서 추가 저장 시 호출합니다.")
    public ResponseEntity<ApiResponse<SensorResponse>> createSensor(@Valid @RequestBody SensorRequest request) {
        SensorResponse response = sensorService.createSensor(SecurityContextHelper.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("센서 생성 성공", response, HttpStatus.CREATED.value()));
    }

    @GetMapping("/equipment/{equipmentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "장비별 센서 목록 조회", description = "프론트 사용 시점: 선택한 장비 아래 센서 목록을 렌더링할 때 호출합니다.")
    public ResponseEntity<ApiResponse<List<SensorResponse>>> getEquipmentSensors(@PathVariable Long equipmentId) {
        List<SensorResponse> response = sensorService.getEquipmentSensors(SecurityContextHelper.getCurrentUserId(), equipmentId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @GetMapping("/{sensorId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "센서 단건 조회", description = "프론트 사용 시점: 센서 상세 패널 또는 수정 모달을 열 때 단건 정보를 읽어올 때 사용합니다.")
    public ResponseEntity<ApiResponse<SensorResponse>> getSensor(@PathVariable Long sensorId) {
        SensorResponse response = sensorService.getSensor(SecurityContextHelper.getCurrentUserId(), sensorId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @PutMapping("/{sensorId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "센서 수정", description = "프론트 사용 시점: 센서명 수정 저장 시 호출합니다.")
    public ResponseEntity<ApiResponse<SensorResponse>> updateSensor(
            @PathVariable Long sensorId,
            @Valid @RequestBody SensorRequest request) {
        SensorResponse response = sensorService.updateSensor(SecurityContextHelper.getCurrentUserId(), sensorId, request);
        return ResponseEntity.ok(ApiResponse.success("수정 성공", response));
    }

    @DeleteMapping("/{sensorId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "센서 삭제", description = "프론트 사용 시점: 센서 삭제 확인 후 실제 제거 시 호출합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteSensor(@PathVariable Long sensorId) {
        sensorService.deleteSensor(SecurityContextHelper.getCurrentUserId(), sensorId);
        return ResponseEntity.ok(ApiResponse.success("삭제 성공"));
    }
}
