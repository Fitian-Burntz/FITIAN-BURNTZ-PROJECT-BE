package com.fitian.burntz.domain.auth.oauth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 개선된 AppleTokenService
 * - client_secret은 AppleClientSecretService 로부터 가져옴 (캐시)
 * - RestTemplate 주입 재사용
 * - 비-2xx 응답 처리 및 id_token 검증 통합
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppleTokenService {

    private static final String TOKEN_URL = "https://appleid.apple.com/auth/token";

    @Value("${spring.security.oauth2.client.registration.apple.client-id}")
    private String clientId;

    private final AppleClientSecretService clientSecretService; // 새로 추가한 서비스 (캐시용)
    private final AppleApiClient appleApiClient;                // id_token 검증용(기존 구현 재사용)
    private final RestTemplate restTemplate;                  // 빈으로 주입하는 것을 권장

    /**
     * code -> 토큰 교환. 반환값은 Apple token endpoint의 응답 body(Map).
     * 내부적으로 id_token 이 반환되면 즉시 검증합니다 (서명, aud, exp 등).
     */
    public Map<String, Object> exchangeCodeForTokens(String code, String redirectUri) {
        String clientSecret = clientSecretService.getClientSecret();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        if (redirectUri != null && !redirectUri.isBlank()) {
            params.add("redirect_uri", redirectUri);
        }
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(TOKEN_URL, request, Map.class);

            if (!resp.getStatusCode().is2xxSuccessful()) {
                String bodyLog = (resp.getBody() != null) ? resp.getBody().toString() : "null";
                log.warn("Apple token endpoint returned non-2xx: {} body={}", resp.getStatusCode(), bodyLog);
                throw new RuntimeException("Failed to exchange code for Apple tokens");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> tokenBody = resp.getBody();
            if (tokenBody == null) {
                log.warn("Apple token endpoint returned empty body for code exchange");
                throw new RuntimeException("Empty response from Apple token endpoint");
            }

            // id_token 이 있으면 검증 (예외 발생 시 호출자에게 전달)
            Object idTokenObj = tokenBody.get("id_token");
            if (idTokenObj instanceof String) {
                String idToken = (String) idTokenObj;
                try {
                    appleApiClient.getUserInfoFromIdToken(idToken); // 검증 및 파싱 (기존 구현 재사용)
                } catch (IllegalArgumentException iae) {
                    log.warn("Apple id_token validation failed: {}", iae.getMessage());
                    throw iae;
                } catch (Exception ex) {
                    log.error("Apple id_token validation error", ex);
                    throw new RuntimeException("invalid apple id_token");
                }
            } else {
                log.debug("No id_token returned in Apple token response (may be present in some cases)");
            }

            return tokenBody;
        } catch (HttpStatusCodeException hsce) {
            // Apple에서 보낸 에러 body는 디버깅에 유용하나 민감 정보 포함 금지
            String respBody = hsce.getResponseBodyAsString();
            log.warn("Apple token endpoint error: status={} body={}", hsce.getStatusCode(), respBody);
            throw new RuntimeException("Failed to exchange code for Apple tokens");
        } catch (RuntimeException re) {
            throw re; // 이미 로깅된 경우 재던짐
        } catch (Exception ex) {
            log.error("Unexpected error while exchanging code for Apple tokens", ex);
            throw new RuntimeException("Failed to exchange code for Apple tokens", ex);
        }
    }
}