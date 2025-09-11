package com.fitian.burntz.domain.auth.oauth;


import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.concurrent.locks.ReentrantLock;

import static com.fitian.burntz.global.common.util.SecureLogUtil.sha256Prefix;

/**
 * Apple client_secret (ES256 JWT)를 생성하고 캐시하는 서비스.
 * - client_secret은 최대 6개월까지 유효(Apple)하므로 캐싱 후 만료 1시간 전에 재생성합니다.
 * - .p8 파일을 직접 읽음. 운영 환경에서는 Secret Manager 사용 권장.
 */
@Service
@Slf4j
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
        if (cachedClientSecret == null) {
            log.info("No cached Apple client_secret found — will generate new one.");
        } else if (now.isAfter(cachedExpiresAt.minusSeconds(3600))) {
            log.info("Cached Apple client_secret is expiring soon (expiresAt={}), regenerating.", cachedExpiresAt);
        } else {
            // 캐시 사용
            log.debug("Using cached Apple client_secret (expiresAt={})", cachedExpiresAt);
            return cachedClientSecret;
        }

        lock.lock();
        try {
            now = Instant.now();
            if (cachedClientSecret == null || now.isAfter(cachedExpiresAt.minusSeconds(3600))) {
                regenerate();
            } else {
                log.debug("Another thread refreshed the client_secret; using updated cache.");
            }
        } finally {
            lock.unlock();
        }
        // 절대 원문을 로그에 남기지 않고, 필요 시 sha prefix만 남김
        if (log.isDebugEnabled() && cachedClientSecret != null) {
            log.debug("Returning client_secret shaPrefix={}", sha256Prefix(cachedClientSecret, 8));
        }
        return cachedClientSecret;
    }

    private void regenerate() {
        try {
            log.info("Regenerating Apple client_secret using private key file: {}", privateKeyPath);
            String pem = Files.readString(Path.of(privateKeyPath));
            String jwt = AppleClientSecretGenerator.generateClientSecret(teamId, clientId, keyId, pem);

            SignedJWT signed = SignedJWT.parse(jwt);
            Date exp = signed.getJWTClaimsSet().getExpirationTime();
            cachedClientSecret = jwt;
            cachedExpiresAt = (exp == null) ? Instant.now().plusSeconds(60 * 60 * 24 * 30) : exp.toInstant();
            log.info("Generated new Apple client_secret; expiresAt={}", cachedExpiresAt);
        } catch (Exception e) {
            log.error("Failed to generate Apple client_secret", e);
            throw new RuntimeException("Failed to generate Apple client_secret", e);
        }
    }

}