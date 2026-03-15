package com.fitian.burntz.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.global.config
 * @fileName : RestTemplateConfig
 * @date : 2026-03-15
 * @description :
 */

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
