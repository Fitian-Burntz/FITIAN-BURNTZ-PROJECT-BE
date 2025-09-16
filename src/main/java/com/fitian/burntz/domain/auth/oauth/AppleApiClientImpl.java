package com.fitian.burntz.domain.auth.oauth;

import com.fitian.burntz.domain.auth.dto.OAuthUserInfo;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jose.util.ResourceRetriever;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;

import java.net.URL;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppleApiClientImpl implements AppleApiClient {


    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String appleClientId;

    @Value("${apple.jwks-url:https://appleid.apple.com/auth/keys}")
    private String appleJwksUrl;

    private volatile JWKSource<SecurityContext> jwkSource;

    private synchronized JWKSource<SecurityContext> getJwkSource() throws Exception {
        if (jwkSource == null) {
            log.debug("Initializing RemoteJWKSet from url={}", appleJwksUrl);
            ResourceRetriever resourceRetriever = new DefaultResourceRetriever(5000, 5000);
            jwkSource = new RemoteJWKSet<>(new URL(appleJwksUrl), resourceRetriever);
        }
        return jwkSource;
    }

    @Override
    public OAuthUserInfo getUserInfoFromIdToken(String idToken) {
        try {
            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            JWSKeySelector<SecurityContext> keySelector =
                    new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, getJwkSource());
            jwtProcessor.setJWSKeySelector(keySelector);

            SecurityContext ctx = null;
            JWTClaimsSet claims = jwtProcessor.process(idToken, ctx);

            // 검증: issuer
            String issuer = claims.getIssuer();
            log.debug("Apple id_token claims: iss='{}', aud={}, exp={}", issuer, claims.getAudience(), claims.getExpirationTime());

            if (!"https://appleid.apple.com".equals(issuer) && !"https://appleid.apple.com/".equals(issuer)) {
                log.warn("Invalid Apple issuer: {}", issuer);
                throw new IllegalArgumentException("Invalid Apple issuer: " + issuer);
            }


            // 검증: audience (appleClientId 포함여부)
            List<String> aud = claims.getAudience();
            if (aud == null || !aud.contains(appleClientId)) {
                log.warn("Invalid audience for Apple id_token. expected='{}' got='{}'", appleClientId, aud);
                throw new IllegalArgumentException("Invalid audience for Apple id_token");
            }

            // MODIFIED: expiry 검사에 clock skew 허용 (예: 60초)
            Date exp = claims.getExpirationTime();
            long now = System.currentTimeMillis();
            final long CLOCK_SKEW_MS = 60_000L; // ADDED
            if (exp == null || exp.getTime() + CLOCK_SKEW_MS < now) { // ADDED (clock skew allowance)
                log.warn("Apple id_token is expired or missing exp: exp={}", exp);
                throw new IllegalArgumentException("Apple id_token is expired");
            }

            // subject (sub) 는 Apple의 고유 사용자 ID
            String sub = claims.getSubject();

            // ADDED: email 파싱을 더 안전하게 처리 (getStringClaim 예외 대비)
            String email = null;
            try {
                email = claims.getStringClaim("email"); // may throw if claim type unexpected
            } catch (Exception ignored) {
                Object eObj = claims.getClaim("email");
                if (eObj != null) {
                    email = String.valueOf(eObj);
                } else {
                    email = null;
                }
            }

            // ADDED: email_verified 다양한 타입(boolean/string 등) 처리
            Boolean emailVerified = null;
            Object emailVerifiedObj = claims.getClaim("email_verified"); // ADDED
            if (emailVerifiedObj != null) { // ADDED
                if (emailVerifiedObj instanceof Boolean) { // ADDED
                    emailVerified = (Boolean) emailVerifiedObj; // ADDED
                } else { // ADDED
                    emailVerified = Boolean.valueOf(String.valueOf(emailVerifiedObj)); // ADDED
                }
            } // ADDED

            log.info("Apple id_token validated: sub='{}' email='{}' email_verified='{}'", sub, email == null ? "(none)" : email, emailVerified);

            return OAuthUserInfo.builder()
                    .memberId(sub)
                    .email(email)
                    .emailVerified(emailVerified) // ADDED
                    .nickname(null)
                    .build();
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception ex) {
            log.error("Failed to validate Apple id_token", ex);
            throw new RuntimeException("Failed to validate Apple id_token", ex);
        }
    }
}