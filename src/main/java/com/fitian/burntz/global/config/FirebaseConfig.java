package com.fitian.burntz.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.global.config
 * @fileName : FirebaseConfig
 * @date : 2025-09-11
 * @description : Firebase Configuration 입니다.
 */

@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account:classpath:serviceAccountKey.json}")
    private Resource serviceAccount;

    @PostConstruct
    public void initialize() throws IOException {
        try (InputStream is = serviceAccount.getInputStream()) {
            var options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(is))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        }
    }

}
