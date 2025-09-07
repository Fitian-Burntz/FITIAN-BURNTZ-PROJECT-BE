package com.fitian.burntz.global.security.jwt;

import com.fitian.burntz.global.security.core.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
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

        log.debug("[JwtTokenFilter] resolved token present? {}", token != null);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long memberPk = jwtTokenProvider.getMemberPkFromToken(token);
            if (memberPk != null) {
                // loadMemberByMemberPk 메서드 사용
                var userDetails = customUserDetailsService.loadMemberByMemberPk(memberPk);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // 먼저 쿠키에서 accessToken 확인하도록 (프론트가 쿠키에 넣어주는 방식에 맞춤) ***
        // 1) 쿠키에서 accessToken 확인
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (StringUtils.hasText(token)) {
                        log.debug("[JwtTokenFilter] token read from cookie, length={}", token.length());
                        return token;
                    }
                }
            }
        }

        // 2) Authorization 헤더에서 확인
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        //쿠키 기반으로 읽고 싶으면 여기 확장
        return null;
    }
}
