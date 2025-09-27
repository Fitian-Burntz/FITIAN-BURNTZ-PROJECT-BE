package com.fitian.burntz.global.common.util;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.global.common.util
 * @fileName : AppleKeyBootstrap
 * @date : 2025-09-25
 * @description : AppleKey 부트스트랩 컴포넌트
 */
@Component
public class AppleKeyBootstrap {

    @Value("${BURNTZ_APPLE_PRIVATE_KEY_PEM:}")
    private String pem; // 시크릿에서 온 PEM 본문

    @Value("${BURNTZ_APPLE_PRIVATE_KEY_FILE:/tmp/apple_private_key.p8}")
    private String keyFilePath; // TaskDef에서 준 경로

    @PostConstruct
    public void init() throws Exception {
        if (pem == null || pem.isBlank()) return;
        String normalized = pem.replace("\\n", "\n").trim(); // \n 복원
        Path path = Path.of(keyFilePath);
        Files.createDirectories(path.getParent());
        Files.writeString(path, normalized);
    }
}