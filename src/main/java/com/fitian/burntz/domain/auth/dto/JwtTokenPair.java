package com.fitian.burntz.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class JwtTokenPair {
    private String accessToken;
    private String refreshToken;
}