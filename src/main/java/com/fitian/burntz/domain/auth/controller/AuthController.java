package com.fitian.burntz.domain.auth.controller;

import com.fitian.burntz.domain.auth.dto.JwtTokenPair;
import com.fitian.burntz.domain.auth.dto.LoginResponse;
import com.fitian.burntz.domain.auth.oauth.OAuthService;
import com.fitian.burntz.domain.auth.service.AuthService;
import com.fitian.burntz.domain.auth.service.RefreshTokenService;
import com.fitian.burntz.domain.member.dto.MemberDto;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.common.response.ApiResponse;
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
    private final OAuthService oAuthService;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.accessTokenExpirationTime}")
    private Long jwtAccessTokenExpirationTime;

    @Value("${jwt.refreshTokenExpirationTime}")
    private Long jwtRefreshTokenExpirationTime;

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> loginWithSocial(
            @RequestHeader("Authorization") String socialTokenHeader,
            @RequestParam("Provider") String provider,
            @RequestBody(required = false) Map<String, String> body) {

        if (socialTokenHeader == null || !socialTokenHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Authorization header required"));
        }
        String token = socialTokenHeader.replaceFirst("^Bearer\\s+", "");

        Member member;
        try {
            member = oAuthService.findOrCreateUserBySocialToken(token, provider);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(iae.getMessage()));
        } catch (Exception e) {
            log.error("social login failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("social login failed"));
        }

        JwtTokenPair pair = jwtTokenProvider.createTokenPair(member);

        // 🔒 deviceId 필수
        String deviceId = (body != null) ? body.get("deviceId") : null;
        if (deviceId == null || deviceId.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("deviceId is required"));
        }

        refreshTokenService.saveOrUpdateRefreshToken(member.getMemberPk(), pair.getRefreshToken(), deviceId.trim());

        LoginResponse loginResponse = LoginResponse.builder()
                .jwtTokenPair(pair)
                .member(MemberDto.from(member))
                .newMember(false)
                .deviceId(deviceId.trim())
                .build();

        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    // 현재 기기만 로그아웃 — deviceId 필수(정확한 행만 제거)
    @PostMapping("/logout")
    public ResponseEntity<?> logoutCurrentDevice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, String> body) {

        String refreshToken = extractBearer(authorization, body);
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("no refresh token provided"));
        }

        // [ADDED] 로그아웃용으로도 반드시 리프레시 토큰인지 확인
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("refresh token required"));
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("invalid refresh token"));
        }

        Long memberPk = jwtTokenProvider.getMemberPkFromRefreshToken(refreshToken); // [CHANGED]
        if (memberPk == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("invalid token subject"));
        }

        if (!refreshTokenService.validateRefreshTokenForMember(memberPk, refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("refresh token mismatch"));
        }

        // 🔒 deviceId 필수
        String deviceId = (body != null) ? body.get("deviceId") : null;
        if (deviceId == null || deviceId.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("deviceId is required"));
        }

        //토큰 기준 삭제 → deviceId 기준 삭제
        boolean deleted = refreshTokenService.deleteByMemberAndDeviceId(memberPk, deviceId.trim());
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("device not found")); // [CHANGED]
        }

        return ResponseEntity.ok(ApiResponse.success(Map.of("result", "logged out", "deviceId", deviceId.trim()))); // [CHANGED]

    }


    /** 모든 기기에서 로그아웃: Authorization 헤더로 액세스/리프레시 아무 토큰 전달 가능 */
    // 전체 로그아웃 — deviceId 불필요
    @PostMapping("/logout/all")
    public ResponseEntity<?> logoutAllDevices(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("no token provided"));
        }
        String anyToken = authorization.substring(7);
        if (!jwtTokenProvider.validateToken(anyToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("invalid token"));
        }
        Long memberPk = jwtTokenProvider.getMemberPkFromToken(anyToken);
        if (memberPk == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("invalid token subject"));
        }
        refreshTokenService.deleteAllByMember(memberPk);
        return ResponseEntity.ok(ApiResponse.success(Map.of("result", "logged out from all devices")));
    }

    // 리프레시 (회전) — deviceId 필수로 맞춤
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshTokenBased(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, String> body) {

        String refreshToken = extractBearer(authorization, body);
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("no refresh token"));
        }

        // [ADDED] 반드시 '리프레시 토큰'인지 확인
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("refresh token required"));
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("invalid refresh token"));
        }

        Long memberPk = jwtTokenProvider.getMemberPkFromRefreshToken(refreshToken); // [CHANGED -> 명시적 메서드 사용]
        if (memberPk == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("invalid token subject"));
        }

        if (!refreshTokenService.validateRefreshTokenForMember(memberPk, refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("refresh token mismatch"));
        }

        // 🔒 deviceId 필수
        String deviceId = (body != null) ? body.get("deviceId") : null;
        if (deviceId == null || deviceId.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("deviceId is required"));
        }

        Member member = memberRepository.findById(memberPk).orElse(null);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("no member"));
        }

        CustomUserDetails principal = new CustomUserDetails(member);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String newAccessToken = jwtTokenProvider.generateAccessToken(auth, jwtAccessTokenExpirationTime);   // [CHANGED]
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(auth, jwtRefreshTokenExpirationTime); // [CHANGED]

        refreshTokenService.saveOrUpdateRefreshToken(memberPk, newRefreshToken, deviceId.trim());

        Map<String, Object> resp = Map.of(
                "accessToken", newAccessToken,
                "accessTokenExpiresIn", jwtAccessTokenExpirationTime / 1000,
                "refreshToken", newRefreshToken,
                "refreshTokenExpiresIn", jwtRefreshTokenExpirationTime / 1000,
                "memberPk", memberPk,
                "deviceId", deviceId.trim()
        );

        return ResponseEntity.ok(ApiResponse.success(resp));
    }

    // POST 로그아웃
    @PostMapping("/cookie/logout/")
    public ResponseEntity<?> logoutPost(HttpServletRequest request, HttpServletResponse response) {
        log.info("[AuthController] logout POST invoked. remote={} method={}", request.getRemoteAddr(), request.getMethod());
        logRequestDebug(request);
        authService.logout(request, response);
        log.info("[AuthController] logout POST done (cookies set to expire).");
        return ResponseEntity.ok().body("logged out");
    }


    /**
     * POST /api/auth/refresh
     * - 쿠키의 refreshToken 사용
     * - 서명/만료 검사 -> memberPk 추출 -> DB 해시 비교 -> 회전(새 access/refresh 발급 + DB 갱신)
     */
    @PostMapping("/cookie/refresh")
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

    /** helper: 헤더 우선, 없으면 body의 "refreshToken" 사용 */
    // 토큰 문자열 추출 (Bearer ... 또는 body.refreshToken)
    private String extractBearer(String authorization, Map<String, String> body) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        if (body != null) {
            String t = body.get("refreshToken");
            if (t != null && !t.isBlank()) return t.trim();
        }
        return null;
    }


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
