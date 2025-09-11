package com.fitian.burntz.domain.auth.controller;

import com.fitian.burntz.domain.auth.dto.JwtTokenPair;
import com.fitian.burntz.domain.auth.dto.LoginResponse;
import com.fitian.burntz.domain.auth.oauth.OAuthService;
import com.fitian.burntz.domain.auth.service.AuthService;
import com.fitian.burntz.domain.auth.service.RefreshTokenService;
import com.fitian.burntz.domain.member.dto.MemberCreateResult;
import com.fitian.burntz.domain.member.dto.MemberDto;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.common.util.SecureLogUtil;
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

        // Authorization 검증
        if (socialTokenHeader == null || !socialTokenHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Authorization header required"));
        }
        String token = socialTokenHeader.replaceFirst("^Bearer\\s+", "");

        // deviceId 필수 검증 — 멤버 생성 이전에 반드시 확인 (CHANGED)
        String deviceId = (body != null) ? body.get("deviceId") : null;
        if (deviceId == null || deviceId.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("deviceId is required"));
        }
        deviceId = deviceId.trim(); // CHANGED: 미리 trim

        // 안전한 로그: 토큰 원문은 기록하지 않음
        if (log.isDebugEnabled()) {
            log.debug("loginWithSocial called provider={} tokenPresent={} tokenHashPrefix={} deviceId={}",
                    provider,
                    token != null,
                    token != null ? SecureLogUtil.sha256Prefix(token, 8) : "(none)",
                    deviceId); // CHANGED: deviceId를 로그에 포함
        }

        // 이제 안전하게 멤버 조회/생성 수행 (deviceId 검증 후 호출하도록 순서 변경) (CHANGED)
        MemberCreateResult createResult;
        try {
            createResult = oAuthService.findOrCreateUserBySocialToken(token, deviceId, provider);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(iae.getMessage()));
        } catch (Exception e) {
            log.error("social login failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("social login failed"));
        }

        Member member = createResult.member();
        boolean isNewMember = createResult.isNewMember();

        JwtTokenPair pair = jwtTokenProvider.createTokenPair(member);
        refreshTokenService.saveOrUpdateRefreshToken(member.getMemberPk(), pair.getRefreshToken(), deviceId);

        LoginResponse loginResponse = LoginResponse.builder()
                .jwtTokenPair(pair)
                .member(MemberDto.from(member))
                .newMember(isNewMember)
                .deviceId(deviceId)
                .build();

        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    // 현재 기기만 로그아웃 — deviceId 필수(정확한 행만 제거)
    @PostMapping("/logout")
    public ResponseEntity<?> logoutCurrentDevice(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, String> body) {

        String refreshToken = extractBearer(authorization, body);

        // 안전한 디버그 로그 (토큰 원문 대신 존재 여부 + 해시접두사)
        if (log.isDebugEnabled()) {
            log.debug("logoutCurrentDevice called refreshTokenPresent={} refreshTokenHashPrefix={}",
                    refreshToken != null && !refreshToken.isBlank(),
                    refreshToken != null ? SecureLogUtil.sha256Prefix(refreshToken, 8) : "(none)");
        }

        ValidationResult validationResult;

        try {
            validationResult = validateRefreshTokenAndDeviceId(refreshToken, body);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(iae.getMessage()));
        }

        //토큰 기준 삭제 → deviceId 기준 삭제
        boolean deleted = refreshTokenService.softDeleteByMemberAndDeviceId(validationResult.memberPk(), validationResult.deviceId());
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("device not found")); // [CHANGED]
        }

        if (log.isDebugEnabled()) {
            log.debug("logoutCurrentDevice success memberPk={} deviceId={}", validationResult.memberPk(), validationResult.deviceId());
        }

        return ResponseEntity.ok(ApiResponse.success(Map.of("result", "logged out", "deviceId", validationResult.deviceId()))); // [CHANGED]

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


        if (log.isDebugEnabled()) {
            log.debug("logoutAllDevices called tokenPresent={} tokenHashPrefix={}",
                    anyToken != null && !anyToken.isBlank(),
                    anyToken != null ? SecureLogUtil.sha256Prefix(anyToken, 8) : "(none)");
        }


        if (!jwtTokenProvider.validateToken(anyToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("invalid token"));
        }

        Long memberPk = jwtTokenProvider.getMemberPkFromToken(anyToken);

        if (memberPk == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("invalid token subject"));
        }
        refreshTokenService.softDeleteAllByMember(memberPk);

        if (log.isDebugEnabled()) {
            log.debug("logoutAllDevices completed memberPk={}", memberPk);
        }

        return ResponseEntity.ok(ApiResponse.success(Map.of("result", "logged out from all devices")));
    }

    // 리프레시 (회전) — deviceId 필수로 맞춤
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshTokenBased(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody(required = false) Map<String, String> body) {


        String refreshToken = extractBearer(authorization, body);

        if (log.isDebugEnabled()) {
            log.debug("refreshTokenBased called refreshTokenPresent={} refreshTokenHashPrefix={}",
                    refreshToken != null && !refreshToken.isBlank(),
                    refreshToken != null ? SecureLogUtil.sha256Prefix(refreshToken, 8) : "(none)");
        }

        ValidationResult validationResult;
        try {
            validationResult = validateRefreshTokenAndDeviceId(refreshToken, body);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(iae.getMessage()));
        }

        Member member = memberRepository.findById(validationResult.memberPk()).orElse(null);

        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("no member"));
        }

        CustomUserDetails principal = new CustomUserDetails(member);

        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String newAccessToken = jwtTokenProvider.generateAccessToken(auth, jwtAccessTokenExpirationTime);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(auth, jwtRefreshTokenExpirationTime);

        refreshTokenService.saveOrUpdateRefreshToken(validationResult.memberPk(), newRefreshToken, validationResult.deviceId());

        Map<String, Object> resp = Map.of(
                "accessToken", newAccessToken,
                "accessTokenExpiresIn", jwtAccessTokenExpirationTime / 1000,
                "refreshToken", newRefreshToken,
                "refreshTokenExpiresIn", jwtRefreshTokenExpirationTime / 1000,
                "memberPk", validationResult.memberPk(),
                "deviceId", validationResult.deviceId()
        );

        if (log.isDebugEnabled()) {
            log.debug("refreshTokenBased success memberPk={} deviceId={} newRefreshHashPrefix={}",
                    validationResult.memberPk(), validationResult.deviceId(), SecureLogUtil.sha256Prefix(newRefreshToken, 8));
        }

        return ResponseEntity.ok(ApiResponse.success(resp));
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


    /**
     * 중복 검증 로직을 한 곳에 모음:
     * - 존재 확인, 토큰 타입(refresh), 서명/만료 검증, subject(memberPk) 추출, DB 일치 검증
     * - 실패 시 IllegalArgumentException 으로 메시지를 던짐 (호출부에서 적절한 ResponseEntity로 변환)
     */
    private Long requireValidRefreshTokenAndGetMemberPk(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("no refresh token provided");
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("refresh token required");
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new IllegalArgumentException("invalid refresh token");
        }

        Long memberPk = jwtTokenProvider.getMemberPkFromRefreshToken(refreshToken);
        if (memberPk == null) {
            throw new IllegalArgumentException("invalid token subject");
        }

        if (!refreshTokenService.validateRefreshTokenForMember(memberPk, refreshToken)) {
            throw new IllegalArgumentException("refresh token mismatch");
        }

        return memberPk;
    }

    /**
     * 중복 제거 헬퍼: refreshToken 유효성 검사 + deviceId 존재/trim 검사
     * 반환: ValidationResult(memberPk, deviceIdTrimmed)
     * 예외: IllegalArgumentException(메시지는 그대로 응답 메시지로 사용됨)
     */
    private ValidationResult validateRefreshTokenAndDeviceId(String refreshToken, Map<String, String> body) {
        Long memberPk = requireValidRefreshTokenAndGetMemberPk(refreshToken);

        String deviceId = (body != null) ? body.get("deviceId") : null;
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId is required");
        }
        return new ValidationResult(memberPk, deviceId.trim());
    }

    /**
     * 결과 컨테이너 — Java 17+ 환경에서 깔끔한 record 사용
     */
    private static record ValidationResult(Long memberPk, String deviceId) {}

}
