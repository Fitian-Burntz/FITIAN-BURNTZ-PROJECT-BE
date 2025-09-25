package com.fitian.burntz.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.global.config
 * @fileName : FirebaseConfig
 * @date : 2025-09-11
 * @description : Firebase Configuration 입니다.
 */

@Configuration
@Slf4j
public class FirebaseConfig {

    // 2순위(로컬/테스트 기본값): 클래스패스 리소스
    @Value("${firebase.service-account:classpath:serviceAccountKey.json}")
    private Resource serviceAccount;

    // 1순위(운영): 시크릿에서 주입한 서비스계정 JSON 본문
    @Value("${FIREBASE_SERVICE_ACCOUNT_JSON:}")
    private String firebaseJson;

    private Firestore firestoreInstance;

    @PostConstruct
    public void initialize() throws Exception {
        try (InputStream is = resolveCredentialsInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(is))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("FirebaseApp initialized (source={})",
                        (firebaseJson != null && !firebaseJson.isBlank())
                                ? "FIREBASE_SERVICE_ACCOUNT_JSON"
                                : "classpath:serviceAccountKey.json");
            }
        }
    }

    private InputStream resolveCredentialsInputStream() throws Exception {
        // 1) ENV에 JSON 문자열이 직접 들어온 경우 (운영)
        if (firebaseJson != null && !firebaseJson.isBlank()) {
            String normalized = firebaseJson.replace("\\n", "\n").trim();
            return new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8));
        }
        // 2) 클래스패스 기본값 (로컬)
        return serviceAccount.getInputStream();
    }

    @Bean
    public Firestore firestore() {
        this.firestoreInstance = FirestoreClient.getFirestore();
        log.info("Firestore bean created: {}", this.firestoreInstance);
        return this.firestoreInstance;
    }

    @PreDestroy
    public void close() {
        if (this.firestoreInstance != null) {
            try {
                log.info("Closing Firestore instance.");
                this.firestoreInstance.close();
            } catch (Exception e) {
                log.warn("Error while closing Firestore", e);
            }
        }
    }
}