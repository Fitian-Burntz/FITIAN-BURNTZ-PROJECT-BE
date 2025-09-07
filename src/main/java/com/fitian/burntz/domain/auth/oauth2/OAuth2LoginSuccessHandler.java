package com.fitian.burntz.domain.auth.oauth2;

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

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

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
        Object memberPk = attributes.get("memberPk");

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

        String redirectUrl = "/index.html";

        if (memberPk != null) {
            redirectUrl += "?memberPk=" + memberPk.toString();
        }

        response.sendRedirect(redirectUrl);

    }
}
