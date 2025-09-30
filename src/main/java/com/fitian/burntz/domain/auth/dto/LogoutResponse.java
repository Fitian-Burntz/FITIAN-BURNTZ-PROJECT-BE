package com.fitian.burntz.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "logout 시 결과와 logout 기기 정보를 반환하는 응답 DTO")
public record LogoutResponse(String result, String deviceId) {}