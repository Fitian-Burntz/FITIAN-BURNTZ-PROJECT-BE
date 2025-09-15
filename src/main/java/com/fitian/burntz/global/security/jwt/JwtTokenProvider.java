package com.fitian.burntz.global.security.jwt;

import com.fitian.burntz.domain.auth.dto.JwtTokenPair;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.global.security.config.SecurityConfig;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey; // JwtKey @Bean 에서 주입

    @Value("${jwt.accessTokenExpirationTime}")
    private Long jwtAccessTokenExpirationTime;

    @Value("${jwt.refreshTokenExpirationTime}")
    private Long jwtRefreshTokenExpirationTime;

    private static final String CLAIM_TOKEN_TYPE = "token_type"; // [ADDED]
    private static final String TOKEN_TYPE_ACCESS = "access";    // [ADDED]
    private static final String TOKEN_TYPE_REFRESH = "refresh";  // [ADDED]

    /** 공통 토큰 생성 — 내부적으로 token_type을 넣지 않음 (deprecated) */
    @Deprecated // [CHANGED] 기존 메서드는 직접 호출 말고 아래 generateAccess/Refresh 사용 권장
    public String generateToken(Authentication authentication, Long expirationMillis) {
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        Map<String, Object> claims = new HashMap<>();
        claims.put("memberPk", principal.getMemberPk());
        claims.put("memberId", principal.getMemberId());

        return Jwts.builder()
                .setSubject(String.valueOf(principal.getMemberPk()))
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    // [ADDED] 액세스 토큰 생성 (token_type=access)
    public String generateAccessToken(Authentication authentication, Long expirationMillis) {
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        Map<String, Object> claims = new HashMap<>();
        claims.put("memberPk", principal.getMemberPk());
        claims.put("memberId", principal.getMemberId());
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS);

        return Jwts.builder()
                .setSubject(String.valueOf(principal.getMemberPk()))
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    // [ADDED] 리프레시 토큰 생성 (token_type=refresh)
    public String generateRefreshToken(Authentication authentication, Long expirationMillis) {
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        Map<String, Object> claims = new HashMap<>();
        claims.put("memberPk", principal.getMemberPk());
        claims.put("memberId", principal.getMemberId());
        claims.put(CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH); // [ADDED]

        return Jwts.builder()
                .setSubject(String.valueOf(principal.getMemberPk()))
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();
    }

    /** Member로부터 액세스/리프레시 페어 생성 — 수정: 새로운 생성기 사용 */
    public JwtTokenPair createTokenPair(Member member) {
        CustomUserDetails principal = new CustomUserDetails(member);
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        //token_type 포함된 생성기 사용
        String accessToken  = generateAccessToken(authentication, jwtAccessTokenExpirationTime);
        String refreshToken = generateRefreshToken(authentication, jwtRefreshTokenExpirationTime);

        return JwtTokenPair.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(jwtAccessTokenExpirationTime / 1000)   // 밀리초 -> 초
                .refreshTokenExpiresIn(jwtRefreshTokenExpirationTime / 1000) // 밀리초 -> 초
                .build();
    }

    /** 토큰에서 memberPk 추출 (claim → subject 순) */
    public Long getMemberPkFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .setAllowedClockSkewSeconds(60)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Object val = claims.get("memberPk");
            if (val instanceof Number) return ((Number) val).longValue();
            if (val instanceof String) {
                try { return Long.valueOf((String) val); } catch (NumberFormatException ignored) {}
            }
            String sub = claims.getSubject();
            if (sub != null) {
                try { return Long.valueOf(sub); } catch (NumberFormatException ignored) {}
            }
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /** 서명/만료 검증 (기본) */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .setAllowedClockSkewSeconds(60)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // [ADDED] 이 토큰이 리프레시 토큰인지 (클레임 기반 검사)
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .setAllowedClockSkewSeconds(60)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 로그: 토큰 원문 X, subject/expiry 만 기록 (디버깅 용도)
            if (log.isDebugEnabled()) {
                log.debug("validateToken SUCCESS - subject={} expiresAt={}", claims.getSubject(), claims.getExpiration());
            }

            String typ = claims.get(CLAIM_TOKEN_TYPE, String.class);
            return TOKEN_TYPE_REFRESH.equals(typ);
        } catch (JwtException | IllegalArgumentException e) {
            if (log.isDebugEnabled()) {
                log.debug("validateToken FAILED - reason={}", e.getMessage());
            }
            return false;
        }
    }

    // 리프레시 토큰에서 memberPk 뽑기(명시적 용도)
    public Long getMemberPkFromRefreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .setAllowedClockSkewSeconds(60)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            Object val = claims.get("memberPk");
            if (val instanceof Number) return ((Number) val).longValue();
            if (val instanceof String) {
                try { return Long.valueOf((String) val); } catch (NumberFormatException ignored) {}
            }
            String sub = claims.getSubject();
            if (sub != null) {
                try { return Long.valueOf(sub); } catch (NumberFormatException ignored) {}
            }
            return null;
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }
}