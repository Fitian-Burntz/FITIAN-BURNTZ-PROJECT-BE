package com.fitian.burntz.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AuthTokenResponse {
    private JwtTokenPair jwtTokenPair; // access/refresh + expires
    private Long memberPk;
    private String deviceId;
}