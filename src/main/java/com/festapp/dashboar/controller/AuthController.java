package com.festapp.dashboar.controller;

import com.festapp.dashboar.dto.*;
import com.festapp.dashboar.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "회원가입, 로그인, 로그아웃 등 인증 관련 API")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<ApiResponse<UserInfoResponse>> signUp(@Valid @RequestBody SignUpRequest request) {
        UserInfoResponse response = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("회원가입 성공", response));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 인증 및 JWT 토큰 발급")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새로운 Access Token 발급")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 토큰")
    })
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshAccessToken(request);
        return ResponseEntity.ok(ApiResponse.success("토큰 재발급 성공", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "현재 사용자 로그아웃")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponse<Void>> logout() {
        authService.logout();
        return ResponseEntity.ok(ApiResponse.success("로그아웃 성공"));
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 인증된 사용자의 정보 조회")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponse<UserInfoResponse>> getMyInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("인증 필요", 401));
        }

        com.festapp.dashboar.security.CustomUserDetails userDetails =
                (com.festapp.dashboar.security.CustomUserDetails) authentication.getPrincipal();

        UserInfoResponse response = authService.getUserInfo(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success("조회 성공", response));
    }

    @PostMapping("/change-password")
    @Operation(summary = "비밀번호 변경", description = "현재 사용자의 비밀번호를 변경합니다")
    @SecurityRequirement(name = "bearer-jwt")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ResponseEntity<ApiResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("인증 필요", 401));
        }

        com.festapp.dashboar.security.CustomUserDetails userDetails =
                (com.festapp.dashboar.security.CustomUserDetails) authentication.getPrincipal();

        authService.changePassword(userDetails.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 변경되었습니다"));
    }
}

