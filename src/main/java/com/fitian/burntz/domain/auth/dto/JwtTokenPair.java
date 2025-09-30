package com.fitian.burntz.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "인증 토큰 정보들을 담는 DTO")
public class JwtTokenPair {
    private String accessToken;
    private String refreshToken;

    // 추가: 초 단위 만료기간
    private long accessTokenExpiresIn;
    private long refreshTokenExpiresIn;
}