package com.festapp.dashboar.controller;

import com.festapp.dashboar.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@Tag(name = "Public", description = "공개 API")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "헬스 체크", description = "서버 상태 확인")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> data = new HashMap<>();
        data.put("status", "UP");
        data.put("timestamp", System.currentTimeMillis());
        data.put("service", "DashBoar");
        
        return ResponseEntity.ok(ApiResponse.success("Server is running", data));
    }
}

