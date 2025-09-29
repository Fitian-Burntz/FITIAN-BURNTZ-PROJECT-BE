package com.fitian.burntz.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.global.config
 * @fileName : RedisConfig
 * @date : 2025-09-25
 * @description : Redis 설정 파일
 */
@Configuration
public class RedisConfig {
    
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }
}