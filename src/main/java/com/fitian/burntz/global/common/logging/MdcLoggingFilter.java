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
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    private static final int MAX_BODY_LOG_LENGTH = 500;
    private static final java.util.Set<String> SENSITIVE_PATHS = java.util.Set.of(
            "/api/v1/auth/login"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long startMs = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        MDC.put("requestId", requestId);
        MDC.put("method", request.getMethod());
        MDC.put("path", request.getRequestURI());

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, MAX_BODY_LOG_LENGTH);

        String queryString = request.getQueryString();
        String fullUri = queryString != null ? request.getRequestURI() + "?" + queryString : request.getRequestURI();

        log.info("→ {} {}", request.getMethod(), fullUri);

        try {
            chain.doFilter(wrappedRequest, response);
        } finally {
            long elapsed = System.currentTimeMillis() - startMs;
            String memberPk = MDC.get("memberPk");
            String body = SENSITIVE_PATHS.contains(request.getRequestURI()) ? "" : extractBody(wrappedRequest);
            String bodyLog = body.isBlank() ? "" : " params=" + body;

            if (memberPk != null) {
                log.info("← {} {} {} member={}{} ({}ms)",
                        response.getStatus(), request.getMethod(), fullUri, memberPk, bodyLog, elapsed);
            } else {
                log.info("← {} {} {}{} ({}ms)",
                        response.getStatus(), request.getMethod(), fullUri, bodyLog, elapsed);
            }
            MDC.clear();
        }
    }

    private String extractBody(ContentCachingRequestWrapper request) {
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("multipart")) {
            return "[multipart]";
        }
        byte[] buf = request.getContentAsByteArray();
        if (buf.length == 0) return "";
        return new String(buf, 0, Math.min(buf.length, MAX_BODY_LOG_LENGTH), StandardCharsets.UTF_8);
    }
}
