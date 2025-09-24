package com.fitian.burntz.domain.auth.docs;

import com.fitian.burntz.domain.auth.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Auth 관련 api 입니다.", description = "login,logout,refresh 인증과 관련된 일을 수행합니다.")
public interface AuthDocs {

    @ApiResponse(
            responseCode = "200",
            description = "호출에 성공한 경우",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value =
                                    """
                                        {
                                            "message": "Test API"
                                        }
                                    """
                    )
            )
    )

    @Operation(summary = "소셜로그인 수행 api 입니다.", description = "google/apple 소셜로그인을 수행 합니다.")
    ResponseEntity<?> loginWithSocial(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("provider") String provider,
            @RequestParam(value = "deviceId", required = false) String deviceId);


    @Operation(summary = "현재 기기에서만 로그아웃 api 입니다.", description = "현재 기기에서만 로그아웃 합니다.")
    ResponseEntity<?> logoutCurrentDevice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "deviceId", required = false) String deviceId);


    @Operation(summary = "모든 기기에서 로그아웃 api 입니다.", description = "해당 멤버의 모든 기기에서 로그아웃 합니다.")
    ResponseEntity<?> logoutAllDevices(@RequestHeader(value = "Authorization", required = false) String authorization);


    @Operation(summary = "refreshToken 갱신 api 입니다.", description = "현재 로그인된 계정의 refreshToken을 갱신합니다.")
    ResponseEntity<?> refreshTokenBased(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "deviceId", required = false) String deviceId);

}
