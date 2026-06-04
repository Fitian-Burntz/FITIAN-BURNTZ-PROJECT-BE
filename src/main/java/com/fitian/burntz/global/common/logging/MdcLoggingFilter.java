package com.fitian.burntz.global.common.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MdcLoggingFilter extends OncePerRequestFilter {

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long startMs = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        MDC.put("requestId", requestId);
        MDC.put("method", request.getMethod());
        MDC.put("path", request.getRequestURI());

        log.info("→ {} {}", request.getMethod(), request.getRequestURI());

        try {
            chain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - startMs;
            String memberPk = MDC.get("memberPk");
            if (memberPk != null) {
                log.info("← {} {} {} member={} ({}ms)",
                        response.getStatus(), request.getMethod(), request.getRequestURI(), memberPk, elapsed);
            } else {
                log.info("← {} {} {} ({}ms)",
                        response.getStatus(), request.getMethod(), request.getRequestURI(), elapsed);
            }
            MDC.clear();
        }
    }
}
