package com.fitian.burntz.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "login, refresh 시에 토큰 정보와 멤버 정보, 기기 정보를 전달하는 응답")
public class AuthTokenResponse {
    private JwtTokenPair jwtTokenPair; // access/refresh + expires
    private Long memberPk;
    private String deviceId;
}