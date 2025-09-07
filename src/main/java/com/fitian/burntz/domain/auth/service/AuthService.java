package com.fitian.burntz.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {

    /** 로그아웃 처리: 세션 무효화, SecurityContext 정리, 쿠키 만료 등 **/
    void logout(HttpServletRequest request, HttpServletResponse response);
}
