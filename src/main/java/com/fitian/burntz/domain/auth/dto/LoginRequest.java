package com.fitian.burntz.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.auth.dto
 * @fileName : LoginRequest
 * @date : 2026-01-17
 * @description : Login 시 소셜 엑세스 토큰, provider, device_id를 담는 dto
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @Schema(description = "소셜 엑세스 토큰")
    private String token;

    @Schema(description = "device_id", example = "5DCD6EFD-5869-4FEA-BC91-32BAC70F55BD")
    private String deviceId;

    @Schema(description = "소셜 로그인 종류", example="GOOGLE")
    private String provider;
}
