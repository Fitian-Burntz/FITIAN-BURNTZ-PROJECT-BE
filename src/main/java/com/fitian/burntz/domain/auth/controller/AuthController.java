package com.fitian.burntz.domain.auth.controller;

import com.fitian.burntz.domain.auth.docs.AuthDocs;
import com.fitian.burntz.domain.auth.dto.AuthTokenResponse;
import com.fitian.burntz.domain.auth.dto.LoginResponse;
import com.fitian.burntz.domain.auth.dto.LogoutResponse;
import com.fitian.burntz.domain.auth.service.AuthService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthDocs {

    private final AuthService authService;

    @Override
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithSocial(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("provider") String provider,
            @RequestParam(value = "deviceId", required = false) String deviceId) {

        //클라이언트로 부터 받은 토큰 검증 및 추출
        String token = extractBearer(authorization);

        // deviceId 값 확인
        if (deviceId == null || deviceId.isEmpty()) {
            throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        //deviceId 정제(공백 제거)
        deviceId = deviceId.trim();

        LoginResponse loginResponse = authService.loginWithSocial(token, provider, deviceId);

        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<?> logoutCurrentDevice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "deviceId", required = false) String deviceId) {

        //클라이언트로 부터 받은 토큰 검증 및 추출
        String refreshToken = extractBearer(authorization);

        if (deviceId == null ||  deviceId.isEmpty()) {
            throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        //deviceId 정제(공백 제거)
        deviceId = deviceId.trim();

        authService.logoutCurrentDevice(refreshToken, deviceId);

        LogoutResponse logoutResponse = new LogoutResponse("logged out", deviceId);

        return ResponseEntity.ok(ApiResponse.success(logoutResponse));
    }

    @Override
    @PostMapping("/logout/all")
    public ResponseEntity<?> logoutAllDevices(@RequestHeader(value = "Authorization", required = false) String authorization) {
        //클라이언트로 부터 받은 토큰 검증 및 추출
        String refreshToken = extractBearer(authorization);

        authService.logoutAllDevices(refreshToken);


        return ResponseEntity.ok(ApiResponse.success("logged out all devices"));
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshTokenBased(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "deviceId", required = false) String deviceId) {

        //클라이언트로 부터 받은 토큰 검증 및 추출
        String refreshToken = extractBearer(authorization);

        if (deviceId == null ||  deviceId.isEmpty()) {
            throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        //deviceId 정제(공백 제거)
        deviceId = deviceId.trim();

        AuthTokenResponse response = authService.refreshTokenBased(refreshToken, deviceId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }


    /** 헬퍼 메서드 **/

    /**토큰 검증 및 추출 **/
    private String extractBearer(String authorization) {
        if (authorization == null) {
            throw new ValidationException(ErrorCode.TOKEN_MISSING); // 헤더 없음
        }
        if (!authorization.startsWith("Bearer ")) {
            throw new ValidationException(ErrorCode.TOKEN_FORMAT_INVALID); // Bearer 없는 경우
        }

        // 토큰 추출
        String token = authorization.substring(7).trim();

        if (token.isEmpty()) {
            throw new ValidationException(ErrorCode.TOKEN_EXTRACTION_FAILED); // Bearer 뒤가 빈 값이 경우
        }

        return token;
    }

}