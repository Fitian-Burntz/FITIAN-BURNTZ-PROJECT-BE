package com.fitian.burntz.global.security.jwt;

import com.fitian.burntz.global.security.config.SecurityConfig;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    public final SecretKey secretKey;

    public String generateToken(Authentication authentication, Long expirationMillis) {
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        Date expiryDate = new Date(System.currentTimeMillis() + expirationMillis);

        Map<String, Object> claims = new HashMap<>();
        claims.put("memberPk", principal.getMemberPk());
        claims.put("memberId", principal.getMemberId());

        return Jwts.builder()
                .setSubject(String.valueOf(principal.getMemberPk()))
                .addClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

    }


    public Long getMemberPkFromToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // 1) claim "memberPk" 우선
            Object val = claims.get("memberPk");
            if (val instanceof Number) {
                return ((Number) val).longValue();
            } else if (val instanceof String) {
                try {
                    return Long.valueOf((String) val);
                } catch (NumberFormatException ignored) { /* continue to fallback */ }
            }

            // 2) fallback: subject에서 시도 (subject는 String)
            String sub = claims.getSubject();
            if (sub != null) {
                try {
                    return Long.valueOf(sub);
                } catch (NumberFormatException ignored) { /* subject가 숫자가 아니면 null 반환 */ }
            }

            return null;
        } catch (JwtException | IllegalArgumentException e) {
            // 토큰 파싱 오류(만료/변조 등) -> null 반환
            return null;
        }

    }


    public boolean validateToken(String token) {
        try{
            Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token);
            return true;
        }
        catch (JwtException | IllegalArgumentException e){
            return false;
        }
    }
}
