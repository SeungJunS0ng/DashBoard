package com.festapp.dashboar.controller;

import com.festapp.dashboar.dto.ApiResponse;
import com.festapp.dashboar.dto.UserInfoResponse;
import com.festapp.dashboar.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "사용자 관리 API")
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    @Operation(summary = "사용자 정보 조회", description = "특정 사용자 정보 조회")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserById(@PathVariable Long userId) {
        UserInfoResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @GetMapping("/username/{username}")
    @Operation(summary = "사용자명으로 조회", description = "사용자명으로 사용자 정보 조회")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getUserByUsername(@PathVariable String username) {
        UserInfoResponse response = userService.getUserByUsername(username);
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @GetMapping
    @Operation(summary = "모든 사용자 조회", description = "모든 사용자 목록 조회 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserInfoResponse>>> getAllUsers() {
        List<UserInfoResponse> response = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "사용자 정보 수정", description = "사용자 정보 수정 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserInfoResponse>> updateUser(
            @PathVariable Long userId,
            @RequestBody UserInfoResponse updateRequest) {
        UserInfoResponse response = userService.updateUser(userId, updateRequest);
        return ResponseEntity.ok(ApiResponse.success("수정 성공", response));
    }

    @PostMapping("/{userId}/activate")
    @Operation(summary = "사용자 활성화", description = "사용자를 활성화합니다 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable Long userId) {
        userService.activateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자가 활성화되었습니다"));
    }

    @PostMapping("/{userId}/deactivate")
    @Operation(summary = "사용자 비활성화", description = "사용자를 비활성화합니다 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable Long userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자가 비활성화되었습니다"));
    }

    @PostMapping("/{userId}/promote")
    @Operation(summary = "관리자로 승격", description = "사용자를 관리자로 승격합니다 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> promoteToAdmin(@PathVariable Long userId) {
        userService.promoteToAdmin(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자가 관리자로 승격되었습니다"));
    }

    @PostMapping("/{userId}/demote")
    @Operation(summary = "일반 사용자로 강등", description = "관리자를 일반 사용자로 강등합니다 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> demoteToUser(@PathVariable Long userId) {
        userService.demoteToUser(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자가 일반 사용자로 강등되었습니다"));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "사용자 삭제", description = "사용자를 영구적으로 삭제합니다 (ADMIN 권한 필요)")
    @SecurityRequirement(name = "bearer-jwt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("사용자가 삭제되었습니다"));
    }
}


