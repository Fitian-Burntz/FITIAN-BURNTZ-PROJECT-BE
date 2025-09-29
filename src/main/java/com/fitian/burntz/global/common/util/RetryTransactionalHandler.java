package com.fitian.burntz.global.common.util;

import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.QueryTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.*;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLTransientException;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * @author : 김남이
 * @packageName : com.fitian.burntz.global.common.util
 * @fileName : RetryTransactionalHandler
 * @date : 2025-09-26
 * @description : 재시도 로직 실행 처리
 */

@Component
@Slf4j
public class RetryTransactionalHandler {

    private final TransactionTemplate txTemplate;

    // 재시도 횟수
    private final int maxAttempts;

    // 재시도 간의 대기 시간
    private final long baseBackoffMillis;

    // 재시도 대기 제한 시간
    private final long maxBackoffMillis;
    private final Predicate<Throwable> isTransient;

    public RetryTransactionalHandler(
            TransactionTemplate txTemplate,
            @Value("${retry.maxAttempts}") int maxAttempts,
            @Value("${retry.baseBackoffMillis}") long baseBackoffMillis,
            @Value("${retry.maxBackoffMillis}") long maxBackoffMillis) {
        this.txTemplate = txTemplate;
        this.maxAttempts = maxAttempts;
        this.baseBackoffMillis = baseBackoffMillis;
        this.maxBackoffMillis = maxBackoffMillis;
        this.isTransient = defaultTransientPredicate();
    }

    public <T> T executeWithRetry(Callable<T> work) {
        int attempt = 0;

        while (true) {
            attempt++;
            try {
                return txTemplate.execute(status -> {
                    try {
                        return work.call();
                    } catch (RuntimeException re) {
                        throw re;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception ex) { // <- Exception만 받음 (Error는 그대로 터져 나감)
                Throwable cause = unwrap(ex);

                // 만약 root cause가 Error라면 그대로 던짐 (복구 불가)
                if (cause instanceof Error) {
                    throw (Error) cause;
                }

                if (!isTransient.test(cause) || attempt >= maxAttempts) {
                    log.warn("RetryTransactionalHandler final failure (attempt={}): {} - {}", attempt, cause.getClass().getName(), cause.getMessage(), cause);
                    if (cause instanceof RuntimeException) throw (RuntimeException) cause;
                    throw new RuntimeException(cause);
                }

                log.warn("Transient exception, will retry (attempt={}): {} - {}", attempt, cause.getClass().getName(), cause.getMessage(), cause);

                sleepWithBackoff(attempt, baseBackoffMillis, maxBackoffMillis);
            }
        }
    }

    private void sleepWithBackoff(int attempt, long baseBackoffMillis, long maxBackoffMillis) {
        long exp;
        try {
            exp = Math.multiplyExact(baseBackoffMillis, 1L << Math.max(0, attempt - 1));
        } catch (ArithmeticException ae) {
            exp = maxBackoffMillis;
        }
        long capped = Math.min(exp, maxBackoffMillis);
        double jitterFactor = ThreadLocalRandom.current().nextDouble(0.75d, 1.25d);
        long sleepMs = Math.max(1L, (long) (capped * jitterFactor));

        try {
            Thread.sleep(sleepMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during retry backoff", ie);
        }
    }

    private Throwable unwrap(Throwable ex) {
        Throwable e = ex;
        while (e.getCause() != null && e != e.getCause()) e = e.getCause();
        return e;
    }

    public static Predicate<Throwable> defaultTransientPredicate() {
        return throwable -> {
            if (throwable == null) return false;

            // JPA / persistence
            if (throwable instanceof PessimisticLockException) return true;
            if (throwable instanceof QueryTimeoutException) return true;

            // Hibernate lock acquisition wrapper (클래스명으로 안전하게 체크)
            String className = throwable.getClass().getName();
            if (className.contains("LockAcquisitionException")) return true;

            // 스프링 DAO 계열 - Pessimistic 계열을 포괄
            if (throwable instanceof CannotAcquireLockException) return true;
            if (throwable instanceof PessimisticLockingFailureException) return true;

            // Optimistic locking (포괄 타입)
            if (throwable instanceof OptimisticLockingFailureException) return true;

            // JDBC transient
            if (throwable instanceof SQLTransientException) return true;

            // 마지막 안전망: 기타 스프링 DataAccess 예외들
            return throwable instanceof DataAccessException;
        };
    }
}