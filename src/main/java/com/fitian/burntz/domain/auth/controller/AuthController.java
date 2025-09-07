package com.fitian.burntz.domain.auth.controller;

import com.fitian.burntz.domain.auth.service.AuthService;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import com.fitian.burntz.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;

    @Value("${jwt.accessTokenExpirationTime}")
    private Long jwtAccessTokenExpirationTime;

    @Value("${jwt.refreshTokenExpirationTime}")
    private Long jwtRefreshTokenExpirationTime;

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    // POST 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logoutPost(HttpServletRequest request, HttpServletResponse response) {
        log.info("[AuthController] logout POST invoked. remote={} method={}", request.getRemoteAddr(), request.getMethod());
        logRequestDebug(request);
        authService.logout(request, response);
        log.info("[AuthController] logout POST done (cookies set to expire).");
        return ResponseEntity.ok().body("logged out");
    }

    // GET 로그아웃 (디버깅용)
    @GetMapping("/logout")
    public ResponseEntity<?> logoutGet(HttpServletRequest request, HttpServletResponse response) {
        return logoutPost(request, response);
    }

    /**
     * POST /api/auth/refresh
     * - 쿠키의 refreshToken 사용
     * - 서명/만료 검사 -> memberPk 추출 -> DB 해시 비교 -> 회전(새 access/refresh 발급 + DB 갱신)
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = readCookie(request, "refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "no refresh token"));
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid refresh token"));
        }

        Long memberPk = jwtTokenProvider.getMemberPkFromToken(refreshToken);
        if (memberPk == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "invalid token subject"));
        }

        // DB 검증 (해시 비교)
        boolean ok = authService.validateRefreshTokenForMember(memberPk, refreshToken);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "refresh token mismatch"));
        }

        // 회원 조회
        Member member = memberRepository.findById(memberPk).orElse(null);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "no member"));
        }

        // Authentication 생성 (CustomUserDetails 사용)
        CustomUserDetails principal = new CustomUserDetails(member);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        // 새 토큰 발급 (만료시간: properties에서 주입된 값)
        String newAccessToken = jwtTokenProvider.generateToken(auth, jwtAccessTokenExpirationTime);
        String newRefreshToken = jwtTokenProvider.generateToken(auth, jwtRefreshTokenExpirationTime);

        // 쿠키 갱신 (기존 동작 유지)
        setCookie(response, "accessToken", newAccessToken, (int) (jwtAccessTokenExpirationTime / 1000));
        setCookie(response, "refreshToken", newRefreshToken, (int) (jwtRefreshTokenExpirationTime / 1000));

        // DB 갱신 (deviceId 사용 가능)
        String deviceId = readCookie(request, "deviceId");
        authService.saveOrUpdateRefreshToken(memberPk, newRefreshToken, deviceId);

        // 추가: 응답 바디로 accessToken 및 만료시간 반환
        long expiresInSeconds = jwtAccessTokenExpirationTime / 1000;
        Map<String, Object> resp = Map.of(
                "result", "ok",
                "accessToken", newAccessToken,
                "expiresIn", expiresInSeconds,
                "memberPk", memberPk
        );

        return ResponseEntity.ok(resp);
    }


    // ---------------- helpers ----------------
    private String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private void setCookie(HttpServletResponse response, String name, String value, int maxAgeSeconds) {
        Cookie c = new Cookie(name, value);
        c.setHttpOnly(true);
        c.setPath("/");
        // c.setSecure(true); // 운영(HTTPS) 환경에서 활성화
        c.setMaxAge(maxAgeSeconds);
        response.addCookie(c);
        // SameSite 처리(일부 서블릿 컨테이너는 직접 지원하지 않음)
        String cookieHeader = String.format("%s=%s; Path=/; Max-Age=%d; HttpOnly; SameSite=Lax",
                name, value, maxAgeSeconds);
        response.addHeader("Set-Cookie", cookieHeader);
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
