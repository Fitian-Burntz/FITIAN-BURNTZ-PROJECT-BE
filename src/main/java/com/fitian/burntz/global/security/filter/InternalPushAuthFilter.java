package com.fitian.burntz.global.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.global.security.filter
 * @fileName : OncePerRequestFilter
 * @date : 2026-01-18
 * @description : firebase 푸시 요청시 필요한 인증 필터입니다.
 */
@Component
public class InternalPushAuthFilter extends OncePerRequestFilter {
    @Value("${internal.push.secret}")
    private String internalSecret;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 🔒 내부 푸시 API만 보호
        if (request.getRequestURI().equals("/api/v1/alarm/push-message")
                && "POST".equalsIgnoreCase(request.getMethod())) {

            String headerSecret = request.getHeader("X-Internal-Secret");

            if (!Objects.equals(internalSecret, headerSecret)) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType("application/json");
                response.getWriter().write("""
                    {
                      "code": "UNAUTHORIZED_INTERNAL_CALL",
                      "message": "Invalid internal secret"
                    }
                """);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
