package com.fitian.burntz.domain.auth.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    // 만약 refresh token 을 DB나 저장소에 관리하면 주입해서 무효화 로직 추가 가능
    // private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // 1) 서버 세션 무효화 (있다면)
        try {
            var session = request.getSession(false);
            if (session != null) {
                session.invalidate();
                log.info("[AuthService] invalidated session");
            }
        } catch (Exception ex) {
            log.warn("[AuthService] session invalidate failed: {}", ex.getMessage());
        }

        // 2) 스프링 시큐리티 컨텍스트 클리어
        SecurityContextHolder.clearContext();
        log.info("[AuthService] cleared SecurityContext");

        // 3) 쿠키 만료
        expireCookie(response, "accessToken", "/");
        expireCookie(response, "refreshToken", "/");
        expireCookie(response, "JSESSIONID", "/");

        // 4) 만약 서버에 저장된 refresh token 등도 있다면 여기서 무효화
        // 예: revokeRefreshTokenForCurrentUser();
        // (구현 시, 현재 사용자 식별자 필요 - Authentication에서 꺼내거나 request에서 꺼냄)
    }

    private void expireCookie(HttpServletResponse response, String name, String path) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath(path);
        cookie.setHttpOnly(true);     // 프론트에서 못 지우게
        cookie.setMaxAge(0);          // 즉시 만료
        // cookie.setSecure(true);    // 배포(HTTPS) 환경에서는 true 로 설정
        response.addCookie(cookie);

        // 브라우저 호환성 때문에 헤더로 한 번 더 추가하는 것도 가능:
        // response.addHeader("Set-Cookie", name + "=; Path=" + path + "; Max-Age=0; HttpOnly; SameSite=Lax");
        log.info("[AuthService] expired cookie: {}", name);
    }
}
