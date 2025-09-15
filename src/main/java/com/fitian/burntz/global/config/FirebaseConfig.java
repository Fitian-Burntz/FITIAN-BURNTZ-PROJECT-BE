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
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.service-account:classpath:serviceAccountKey.json}")
    private Resource serviceAccount;

    private Firestore firestoreInstance;

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


    /** firebase 빌드 문제로 추가한 부분 from.남이 **/
    // <-- 핵심: 이 메서드가 Spring Bean 으로 Firestore 인스턴스를 등록해 줍니다 -->
    @Bean
    public Firestore firestore() {
        // FirestoreClient.getFirestore()는 FirebaseApp 초기화 후에 호출해야 합니다.
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
