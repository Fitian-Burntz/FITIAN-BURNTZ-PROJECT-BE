package com.fitian.burntz.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author : 김남이
 * @packageName : com.fitian.burntz.global.config
 * @fileName : TransactionConfig
 * @date : 2025-09-26
 * @description : Transaction 재시도 로직을 위해 TransactionTemplate 빈 등록
 */

@Configuration
public class TransactionConfig {

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager txManager) {
        TransactionTemplate tt = new TransactionTemplate(txManager);
        // 필요시 격리/전파 수준 설정
        tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        // tt.setIsolationLevel(...);
        return tt;
    }
}
