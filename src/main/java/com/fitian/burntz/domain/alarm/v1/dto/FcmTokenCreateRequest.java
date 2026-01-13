package com.fitian.burntz.domain.alarm.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.alarm.v1.dto
 * @fileName : FcmTokenCreateRequest
 * @date : 2026-01-12
 * @description : FcmToken 생성 요청 dto 입니다.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FcmTokenCreateRequest {

    @NotBlank(message = "deviceId must not be blank")
    @Schema(description = "device_id", example = "5DCD6EFD-5869-4FEA-BC91-32BAC70F55BD")
    private String deviceId;

    @NotBlank(message = "token must not be blank")
    @Schema(description = "token", example = "bdf89cbb6895dc4a92829d1b1b7d3995e19b23517422f79afd7caf2aed8b526d")
    private String token;
}
