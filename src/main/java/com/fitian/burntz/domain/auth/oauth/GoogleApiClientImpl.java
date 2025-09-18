package com.fitian.burntz.domain.auth.oauth;

import com.fitian.burntz.domain.auth.dto.OAuthUserInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@Slf4j
public class GoogleApiClientImpl implements GoogleApiClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String GOOGLE_USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";

    @Override
    @SuppressWarnings("unchecked")
    public OAuthUserInfo getUserInfo(String accessToken) {

        // 절대 토큰 원문을 로그에 남기지 않습니다. (필요시 sha256Prefix 로그만)
        if (accessToken == null) {
            throw new IllegalArgumentException("accessToken required");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    GOOGLE_USERINFO_URL,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );
            Map<String, Object> body = response.getBody();
            if (body == null) throw new RuntimeException("Empty response from Google userinfo");

            String sub = (String) body.get("sub");
            String email = (String) body.get("email");
            Object emailVerifiedObj = body.get("email_verified");
            Boolean emailVerified = null;
            if (emailVerifiedObj instanceof Boolean) {
                emailVerified = (Boolean) emailVerifiedObj;
            } else if (emailVerifiedObj != null) {
                emailVerified = Boolean.valueOf(String.valueOf(emailVerifiedObj));
            }
            String name = (String) body.get("name");

            return OAuthUserInfo.builder()
                    .memberId(sub)
                    .email(email)
                    .emailVerified(emailVerified)
                    .nickname(name)
                    .build();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to fetch Google userinfo", ex);
        }
    }
}