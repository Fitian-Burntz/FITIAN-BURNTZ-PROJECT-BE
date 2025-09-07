package com.fitian.burntz.domain.auth.oauth2;

import com.fitian.burntz.domain.auth.service.AuthService; // --- CHANGED: import 추가 ---
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID; // --- CHANGED: import 추가 ---

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AuthService authService;


    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        // pattern-matching + negation 문제 회피 — 안전하게 principal을 먼저 꺼내고 instanceof 체크
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof DefaultOAuth2User)) {
            response.sendRedirect("/");
            return;
        }

        DefaultOAuth2User oAuth2User = (DefaultOAuth2User) principal;

        Map<String, Object> attributes = oAuth2User.getAttributes();

        // attributes 로그 (토큰/멤버PK 포함 여부 확인) ***
        log.info("[OAuth2LoginSuccessHandler] attributes keys: {}", attributes.keySet());
        log.info("[OAuth2LoginSuccessHandler] attributes sample: memberPk={}, accessTokenPresent={}, refreshTokenPresent={}",
                attributes.get("memberPk"),
                attributes.containsKey("accessToken"),
                attributes.containsKey("refreshToken")
        );

        String accessToken = (String) attributes.get("accessToken");
        String refreshToken = (String) attributes.get("refreshToken");
        Object memberPkObj = attributes.get("memberPk"); // --- CHANGED: 변수명 memberPk -> memberPkObj로 변경(파싱 전 보관) ---

        if(accessToken != null) {
            Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
            accessTokenCookie.setHttpOnly(true);
            accessTokenCookie.setPath("/");
//            accessTokenCookie.setSecure(true); //https 환경에서는 활성화
            response.addCookie(accessTokenCookie);
        }

        if (refreshToken != null) {
            Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
            refreshTokenCookie.setHttpOnly(true);
            refreshTokenCookie.setPath("/");
            response.addCookie(refreshTokenCookie);
        }

        // --- deviceId 쿠키 읽기/생성 (기기 식별자 관리용) ---
        String deviceId = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("deviceId".equals(c.getName())) {
                    deviceId = c.getValue();
                    break;
                }
            }
        }
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString();
            Cookie deviceCookie = new Cookie("deviceId", deviceId);
            deviceCookie.setPath("/");
            deviceCookie.setHttpOnly(false); // 프론트에서 읽어야 한다면 false (프론트 관리 방식에 따라 조정)
            // deviceCookie.setSecure(true); // 운영환경(HTTPS)에서는 활성화 고려
            response.addCookie(deviceCookie);
        }
        // --- END ---

        // --- memberPk 안전 파싱 (Number 또는 String 처리) ---
        Long memberPk = null;
        if (memberPkObj instanceof Number) {
            memberPk = ((Number) memberPkObj).longValue();
        } else if (memberPkObj instanceof String) {
            try {
                memberPk = Long.valueOf((String) memberPkObj);
            } catch (NumberFormatException ignored) { }
        }
        // --- END ---

        // --- DB에 refreshToken 저장 호출 (AuthService 사용) ---
        if (memberPk != null && refreshToken != null) {
            try {
                authService.saveOrUpdateRefreshToken(memberPk, refreshToken, deviceId);
            } catch (Exception e) {
                // 저장 실패해도 로그인 흐름은 막지 않음. 필요 시 정책 변경
                log.warn("[OAuth2LoginSuccessHandler] failed to save refresh token for memberPk={} deviceId={} err={}",
                        memberPk, deviceId, e.getMessage());
            }
        }
        // --- END ---

        String redirectUrl = "/index.html";

        if (memberPk != null) {
            redirectUrl += "?memberPk=" + memberPk.toString();
        }

        response.sendRedirect(redirectUrl);

    }
}