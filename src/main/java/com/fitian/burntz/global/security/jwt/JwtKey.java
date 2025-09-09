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
     * application.yml / application.properties 의 jwt.secret 값을 그대로 사용.
     * (짧아도 됨 → SHA-512 해싱으로 강제 64바이트 키로 변환)
     */
    @Value("${jwt.secret:}")
    private String rawSecret;

    @Bean
    public SecretKey secretKey() {
        if (rawSecret == null || rawSecret.isBlank()) {
            throw new IllegalStateException("Missing jwt.secret. 환경변수나 yml/properties에 jwt.secret 설정 필요.");
        }

        try {
            // 문자열 → SHA-512 다이제스트 (64 bytes)
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            byte[] keyBytes = digest.digest(rawSecret.getBytes(StandardCharsets.UTF_8));

            // HS512에 적합한 Key 객체 생성
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-512 알고리즘을 사용할 수 없음", e);
        }
    }
}
