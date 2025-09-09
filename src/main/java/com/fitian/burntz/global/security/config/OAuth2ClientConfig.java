package com.fitian.burntz.global.security.config;

import com.fitian.burntz.domain.auth.oauth.AppleClientSecretService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
public class OAuth2ClientConfig {

    @Value("${spring.security.oauth2.client.provider.apple.authorization-uri:https://appleid.apple.com/auth/authorize}")
    private String authorizationUri;

    @Value("${spring.security.oauth2.client.provider.apple.token-uri:https://appleid.apple.com/auth/token}")
    private String tokenUri;

    @Value("${spring.security.oauth2.client.provider.apple.jwk-set-uri:https://appleid.apple.com/auth/keys}")
    private String jwkSetUri;

    private final AppleClientSecretService appleClientSecretService;

    public OAuth2ClientConfig(AppleClientSecretService appleClientSecretService) {
        this.appleClientSecretService = appleClientSecretService;
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${spring.security.oauth2.client.registration.apple.client-id}") String clientId
    ) {
        // 동적 생성된 client_secret 사용
        String clientSecret = appleClientSecretService.getClientSecret();

        ClientRegistration apple = ClientRegistration.withRegistrationId("apple")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "email")
                .authorizationUri(authorizationUri)
                .tokenUri(tokenUri)
                .jwkSetUri(jwkSetUri)
                .build();

        return new InMemoryClientRegistrationRepository(apple);
    }
}