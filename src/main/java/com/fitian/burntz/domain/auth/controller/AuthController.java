package com.fitian.burntz.domain.auth.controller;

import com.fitian.burntz.domain.auth.docs.AuthDocs;
import com.fitian.burntz.domain.auth.dto.AuthTokenResponse;
import com.fitian.burntz.domain.auth.dto.LoginResponse;
import com.fitian.burntz.domain.auth.dto.LogoutResponse;
import com.fitian.burntz.domain.auth.service.AuthService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.common.util.PreconditionValidator;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthDocs {

    private final AuthService authService;
    private final PreconditionValidator preconditionValidator;

    @Override
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithSocial(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("provider") String provider,
            @RequestParam(value = "deviceId", required = false) String deviceId) {

        //클라이언트로 부터 받은 토큰 검증 및 추출
        String token = extractBearer(authorization);

        // deviceId 정제 및 null 검증
        String targetDeviceId = preconditionValidator.requireDeviceId(deviceId);

        LoginResponse loginResponse = authService.loginWithSocial(token, provider, targetDeviceId);

        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logoutCurrentDevice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "deviceId", required = false) String deviceId) {

        //클라이언트로 부터 받은 토큰 검증 및 추출
        String refreshToken = extractBearer(authorization);

        // deviceId 정제 및 null 검증
        String targetDeviceId = preconditionValidator.requireDeviceId(deviceId);

        // 결과로 로그아웃된 기기 정보 받음
        String loggedOutDevice = authService.logoutCurrentDevice(refreshToken, targetDeviceId);

        return ResponseEntity.ok(ApiResponse.success(new LogoutResponse("logged out", loggedOutDevice)));
    }

    @Override
    @PostMapping("/logout/all")
    public ApiResponse<Void> logoutAllDevices(@RequestHeader(value = "Authorization", required = false) String authorization) {
        //클라이언트로 부터 받은 토큰 검증 및 추출
        String refreshToken = extractBearer(authorization);

        authService.logoutAllDevices(refreshToken);


        return ApiResponse.success(null,"logged out all devices");
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refreshTokenBased(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "deviceId", required = false) String deviceId) {

        //클라이언트로 부터 받은 토큰 검증 및 추출
        String refreshToken = extractBearer(authorization);

        // deviceId 정제 및 null 검증
        String targetDeviceId = preconditionValidator.requireDeviceId(deviceId);

        AuthTokenResponse refreshTokenResponse = authService.refreshTokenBased(refreshToken, targetDeviceId);

        return ResponseEntity.ok(ApiResponse.success(refreshTokenResponse));
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
            throw new ValidationException(ErrorCode.TOKEN_EXTRACTION_FAILED); // Bearer 뒤가 빈 값인 경우
        }

        return token;
    }

}