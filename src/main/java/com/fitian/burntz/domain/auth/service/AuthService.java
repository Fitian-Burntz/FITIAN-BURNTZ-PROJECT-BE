package com.fitian.burntz.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    /** 로그아웃 처리: 세션 무효화, SecurityContext 정리, 쿠키 만료 등 **/
    void logout(HttpServletRequest request, HttpServletResponse response);

    // refresh token 저장/검증/무효화 관련 메서드
    void saveOrUpdateRefreshToken(Long memberPk, String rawRefreshToken, String deviceId);
    boolean validateRefreshTokenForMember(Long memberPk, String rawRefreshToken);
    void revokeRefreshToken(Long memberPk, String deviceId);
    void revokeAllRefreshTokensForMember(Long memberPk);
}
