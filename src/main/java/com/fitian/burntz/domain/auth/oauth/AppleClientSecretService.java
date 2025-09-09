package com.fitian.burntz.domain.auth.oauth;


import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Apple client_secret (ES256 JWT)를 생성하고 캐시하는 서비스.
 * - client_secret은 최대 6개월까지 유효(Apple)하므로 캐싱 후 만료 1시간 전에 재생성합니다.
 * - .p8 파일을 직접 읽음. 운영 환경에서는 Secret Manager 사용 권장.
 */
@Service
public class AppleClientSecretService {

    @Value("${apple.private-key-path}")
    private String privateKeyPath;

    @Value("${apple.team-id}")
    private String teamId;

    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String clientId;

    @Value("${apple.key-id}")
    private String keyId;

    // 캐시된 client_secret (compact JWT)
    private volatile String cachedClientSecret;
    // client_secret 만료 시각
    private volatile Instant cachedExpiresAt = Instant.EPOCH;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * 안전하게 캐시된 client_secret 반환.
     * 필요하면 내부에서 재생성(regenerate)함.
     */
    public String getClientSecret() {
        Instant now = Instant.now();
        // 만료 1시간 전 미리 갱신
        if (cachedClientSecret == null || now.isAfter(cachedExpiresAt.minusSeconds(3600))) {
            lock.lock();
            try {
                // double-check
                now = Instant.now();
                if (cachedClientSecret == null || now.isAfter(cachedExpiresAt.minusSeconds(3600))) {
                    regenerate();
                }
            } finally {
                lock.unlock();
            }
        }
        return cachedClientSecret;
    }

    private void regenerate() {
        try {
            // .p8 파일을 읽어서 JWT 생성 (재사용 가능한 유틸/생성기를 호출)
            String pem = Files.readString(Path.of(privateKeyPath));
            // 재사용: 기존에 만든 AppleClientSecretGenerator 유틸의 generateClientSecret(...) 호출
            String jwt = AppleClientSecretGenerator.generateClientSecret(teamId, clientId, keyId, pem);

            // 만료시간 추출해서 캐시 만료시각 설정
            SignedJWT signed = SignedJWT.parse(jwt);
            Date exp = signed.getJWTClaimsSet().getExpirationTime();
            cachedClientSecret = jwt;
            cachedExpiresAt = (exp == null) ? Instant.now().plusSeconds(60 * 60 * 24 * 30) : exp.toInstant();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Apple client_secret", e);
        }
    }
}