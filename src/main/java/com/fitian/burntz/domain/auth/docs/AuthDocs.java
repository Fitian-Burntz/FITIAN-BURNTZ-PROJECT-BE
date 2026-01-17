package com.fitian.burntz.domain.auth.docs;

import com.fitian.burntz.domain.auth.dto.AuthTokenResponse;
import com.fitian.burntz.domain.auth.dto.LoginRequest;
import com.fitian.burntz.domain.auth.dto.LoginResponse;
import com.fitian.burntz.domain.auth.dto.LogoutResponse;
import com.fitian.burntz.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Auth 관련 api 입니다.", description = "login,logout,refresh 인증과 관련된 일을 수행합니다.")
public interface AuthDocs {

    @Operation(summary = "소셜로그인 수행", description = "google/apple 소셜로그인을 수행 합니다.")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithSocial(
            @Valid @RequestBody LoginRequest request);


    @Operation(summary = "현재 기기에서만 로그아웃", description = "현재 기기에서만 로그아웃 합니다.")
    public ResponseEntity<ApiResponse<LogoutResponse>> logoutCurrentDevice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "deviceId", required = false) String deviceId);


    @Operation(summary = "모든 기기에서 로그아웃", description = "해당 멤버의 모든 기기에서 로그아웃 합니다.")
    public ApiResponse<Void> logoutAllDevices(@RequestHeader(value = "Authorization", required = false) String authorization);


    @Operation(summary = "refreshToken 갱신", description = "현재 로그인된 계정의 refreshToken을 갱신합니다.")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refreshTokenBased(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "deviceId", required = false) String deviceId);

}
