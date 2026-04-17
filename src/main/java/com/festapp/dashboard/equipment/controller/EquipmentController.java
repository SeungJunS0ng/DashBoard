package com.festapp.dashboard.equipment.controller;

import com.festapp.dashboard.common.dto.ApiResponse;
import com.festapp.dashboard.common.security.SecurityContextHelper;
import com.festapp.dashboard.equipment.dto.EquipmentRequest;
import com.festapp.dashboard.equipment.dto.EquipmentResponse;
import com.festapp.dashboard.equipment.service.EquipmentService;
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
@RequestMapping("/api/equipment")
@RequiredArgsConstructor
@Tag(name = "Equipment", description = "대시보드 아래에 속한 장비를 관리하는 API입니다. 프론트에서는 대시보드 편집 화면이나 장비 목록 패널에서 주로 사용합니다.")
@SecurityRequirement(name = "bearer-jwt")
public class EquipmentController {

    private final EquipmentService equipmentService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "장비 생성", description = "프론트 사용 시점: 대시보드 편집 화면에서 장비 추가 모달 저장 시 호출합니다.")
    public ResponseEntity<ApiResponse<EquipmentResponse>> createEquipment(@Valid @RequestBody EquipmentRequest request) {
        EquipmentResponse response = equipmentService.createEquipment(SecurityContextHelper.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("장비 생성 성공", response, HttpStatus.CREATED.value()));
    }

    @GetMapping("/dashboard/{dashboardId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "대시보드별 장비 목록 조회", description = "프론트 사용 시점: 선택한 대시보드 아래 장비 목록을 렌더링할 때 호출합니다.")
    public ResponseEntity<ApiResponse<List<EquipmentResponse>>> getDashboardEquipment(@PathVariable Long dashboardId) {
        List<EquipmentResponse> response = equipmentService.getDashboardEquipment(SecurityContextHelper.getCurrentUserId(), dashboardId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @GetMapping("/{equipmentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "장비 단건 조회", description = "프론트 사용 시점: 장비 상세 패널 또는 수정 모달을 열 때 단건 정보를 읽어올 때 사용합니다.")
    public ResponseEntity<ApiResponse<EquipmentResponse>> getEquipment(@PathVariable Long equipmentId) {
        EquipmentResponse response = equipmentService.getEquipment(SecurityContextHelper.getCurrentUserId(), equipmentId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @PutMapping("/{equipmentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "장비 수정", description = "프론트 사용 시점: 장비명/분류 수정 저장 시 호출합니다.")
    public ResponseEntity<ApiResponse<EquipmentResponse>> updateEquipment(
            @PathVariable Long equipmentId,
            @Valid @RequestBody EquipmentRequest request) {
        EquipmentResponse response = equipmentService.updateEquipment(SecurityContextHelper.getCurrentUserId(), equipmentId, request);
        return ResponseEntity.ok(ApiResponse.success("수정 성공", response));
    }

    @DeleteMapping("/{equipmentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "장비 삭제", description = "프론트 사용 시점: 장비 삭제 확인 후 실제 제거 시 호출합니다.")
    public ResponseEntity<ApiResponse<Void>> deleteEquipment(@PathVariable Long equipmentId) {
        equipmentService.deleteEquipment(SecurityContextHelper.getCurrentUserId(), equipmentId);
        return ResponseEntity.ok(ApiResponse.success("삭제 성공"));
    }
}
