package com.fitian.burntz.global.security.jwt;

import com.fitian.burntz.global.security.core.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    // throws Exception 은 부모 클래스에서 처리됨.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException{
        String path = request.getRequestURI();

        return path.equals("/") ||

                path.startsWith("/api/auth/") ||
                path.startsWith("/oauth2/")||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/fonts/") ||
                path.startsWith("/images/") ||

                path.endsWith(".html");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String token = resolveToken(request);



    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");

        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        //쿠키 기반으로 읽고 싶으면 여기 확장
        return null;
    }
}
