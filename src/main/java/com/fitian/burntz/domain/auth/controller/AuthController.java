package com.fitian.burntz.domain.auth.controller;

import com.fitian.burntz.domain.auth.dto.LoginResponse;
import com.fitian.burntz.domain.auth.service.AuthService;
import com.fitian.burntz.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithSocial(
            @RequestHeader("Authorization") String socialTokenHeader,
            @RequestParam("Provider") String provider,
            @RequestBody(required = false) Map<String, String> body) {

        if (socialTokenHeader == null || !socialTokenHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Authorization header required"));
        }
        String token = socialTokenHeader.replaceFirst("^Bearer\\s+", "");

        String deviceId = (body != null) ? body.get("deviceId") : null;

        LoginResponse resp = authService.loginWithSocial(token, provider, deviceId);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutCurrentDevice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, String> body) {

        String refreshToken = extractBearer(authorization, body);
        String deviceId = (body != null) ? body.get("deviceId") : null;

        authService.logoutCurrentDevice(refreshToken, deviceId);

        return ResponseEntity.ok(ApiResponse.success(Map.of("result", "logged out", "deviceId", deviceId)));
    }

    @PostMapping("/logout/all")
    public ResponseEntity<?> logoutAllDevices(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("no token provided"));
        }
        String anyToken = authorization.substring(7);
        authService.logoutAllDevices(anyToken);
        return ResponseEntity.ok(ApiResponse.success(Map.of("result", "logged out from all devices")));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshTokenBased(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, String> body) {

        String refreshToken = extractBearer(authorization, body);
        String deviceId = (body != null) ? body.get("deviceId") : null;

        Map<String, Object> resp = authService.refreshTokenBased(refreshToken, deviceId);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    // helper: 헤더 우선, 없으면 body의 "refreshToken" 사용
    private String extractBearer(String authorization, Map<String, String> body) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        if (body != null) {
            String t = body.get("refreshToken");
            if (t != null && !t.isBlank()) return t.trim();
        }
        return null;
    }
}