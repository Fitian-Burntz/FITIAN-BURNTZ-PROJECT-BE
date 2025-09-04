package com.fitian.burntz.global.security.jwt;

import com.fitian.burntz.global.security.config.SecurityConfig;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.JwtException;
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
        claims.put("memberId", principal.getUsername());

        return Jwts.builder()
                .setSubject(principal.getUsername())
                .addClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS512)
                .compact();

    }


    public Long getMemberPkFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Object val = claims.get("memberPk");
        if(val instanceof Number) {
            return ((Number)val).longValue();
        }
        else if (val instanceof String) {
            return Long.valueOf((String) val);

        }
        else {
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
