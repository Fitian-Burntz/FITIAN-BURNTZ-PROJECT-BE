package com.fitian.burntz.domain.auth.controller;

import com.fitian.burntz.domain.auth.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    // POST 로그아웃 (정상 처리용)
    @PostMapping("/logout")
    public ResponseEntity<?> logoutPost(HttpServletRequest request, HttpServletResponse response) {
        log.info("[AuthController] logout POST invoked. remote={} method={}", request.getRemoteAddr(), request.getMethod());
        logRequestDebug(request);
        authService.logout(request, response);
        log.info("[AuthController] logout POST done (cookies set to expire).");
        return ResponseEntity.ok().body("logged out");
    }

    // GET도 추가(디버깅 편의). 실제 배포 땐 제거하거나 POST만 허용하세요.
    @GetMapping("/logout")
    public ResponseEntity<?> logoutGet(HttpServletRequest request, HttpServletResponse response) {
        log.info("[AuthController] logout GET invoked. remote={} method={}", request.getRemoteAddr(), request.getMethod());
        logRequestDebug(request);
        authService.logout(request, response);
        log.info("[AuthController] logout GET done (cookies set to expire).");
        return ResponseEntity.ok().body("logged out (get)");
    }

    private void logRequestDebug(HttpServletRequest request) {
        var principal = request.getUserPrincipal();
        log.info("[AuthController] principal={}", principal==null? "null" : principal.getName());
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            log.info("[AuthController] request cookies: none");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Cookie c : cookies) {
                sb.append(c.getName()).append("=").append(c.getValue()).append("; ");
            }
            log.info("[AuthController] request cookies: {}", sb.toString());
        }
    }
}
