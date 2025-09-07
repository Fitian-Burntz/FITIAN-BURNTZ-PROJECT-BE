package com.fitian.burntz.global.security.jwt;

import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;


@Configuration
public class JwtKey {

    /**
     * 반드시 base64 로 인코딩된 64바이트(512비트) 키를 환경변수 또는 application.yml 의
     * jwt.base64-secret 에 설정해야 합니다.
     */
    @Value("${jwt.base64-secret:}")
    private String base64Secret;

    @Bean
    public SecretKey secretKey() {
        if (base64Secret == null || base64Secret.isBlank()) {
            throw new IllegalStateException("Missing jwt.base64-secret. Set a base64-encoded 64-byte key (e.g. openssl rand -base64 64).");
        }

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Secret);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("jwt.base64-secret is not valid base64", e);
        }

        // Keys.hmacShaKeyFor will validate length (HS512 needs >= 512 bits)
        try {
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            // 명확한 에러 메시지로 운영자에게 안내
            throw new IllegalStateException("jwt.base64-secret must decode to at least 64 bytes (512 bits) for HS512. Generate with: openssl rand -base64 64", e);
        }
    }
}
