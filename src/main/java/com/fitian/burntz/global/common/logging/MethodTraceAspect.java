package com.fitian.burntz.global.common.logging;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.global.common.logging
 * @fileName : MethodTraceAspect
 * @date : 2026-01-19
 * @description : 메서드 추적 aspect 입니다.
 */

@Slf4j
@Aspect
@Component
public class MethodTraceAspect {
    // 컨트롤러 + 서비스만 추적 (리포지토리까지 찍으면 너무 시끄러움)
    @Around("within(com.fitian.burntz..*Controller) || within(com.fitian.burntz..*Service..*)")
    public Object trace(ProceedingJoinPoint pjp) throws Throwable {
        String sig = pjp.getSignature().toShortString();
        log.info("▶ {}", sig);
        try {
            Object result = pjp.proceed();
            log.info("✔ {}", sig);
            return result;
        } catch (Exception e) {
            log.warn("✖ {} -> {}", sig, e.toString());
            throw e;
        }
    }
}
