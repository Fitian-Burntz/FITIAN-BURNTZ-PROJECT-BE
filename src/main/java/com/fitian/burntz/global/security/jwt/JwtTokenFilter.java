package com.fitian.burntz.global.security.jwt;

import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.security.core.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static com.fitian.burntz.global.security.handler.RestAuthenticationEntryPoint.ATTR_ERROR_CODE;

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
                path.equals("/api/v1/auth/login") ||
                path.startsWith("/oauth2/")||
                path.startsWith("/css/") ||
                path.startsWith("/js/") ||
                path.startsWith("/fonts/") ||
                path.startsWith("/images/") ||
                path.startsWith("/api/v1/alarm/push-message") ||

                path.endsWith(".html");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String token = resolveToken(request);
        log.debug("[JwtTokenFilter] resolved token present? {}", token != null);

        try {
            if (StringUtils.hasText(token)) {

                jwtTokenProvider.parseClaimsOrThrow(token);

                // 필터가 이미 인증을 세팅했으면 건너뜀
                if (SecurityContextHolder.getContext().getAuthentication() == null) {

                    // (선택) 토큰이 리프레시 토큰인지 확인하고, 리프레시면 무시
                    if (jwtTokenProvider.isRefreshToken(token)) {
                        log.debug("Token is a refresh token - skipping authentication in JwtTokenFilter");
                    } else {
                        Long memberPk = jwtTokenProvider.getMemberPkFromToken(token);
                        if (memberPk != null) {
                            try {
                                var userDetails = customUserDetailsService.loadMemberByMemberPk(memberPk);
                                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities()
                                );
                                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                                log.debug("JWT authenticated memberPk={}", memberPk);
                            } catch (UsernameNotFoundException usernameNotFoundException) {
                                log.warn("Member not found for memberPk={} - skipping authentication", memberPk);
                            }
                        }
                    }
                }
            }
        } catch(ExpiredJwtException e) {
            SecurityContextHolder.clearContext();
            request.setAttribute(ATTR_ERROR_CODE, ErrorCode.TOKEN_EXPIRED);
        } catch (Exception e) {
            // 토큰 검증 실패 등 모든 예외는 여기서 잡아서 무시(다음 필터로 넘어가게)하거나
            // 필요하면 response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            SecurityContextHolder.clearContext();
            request.setAttribute(ATTR_ERROR_CODE, ErrorCode.TOKEN_INVALID);
            log.warn("JwtTokenFilter error: {}", e.getMessage());
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

        return null;
    }
}
