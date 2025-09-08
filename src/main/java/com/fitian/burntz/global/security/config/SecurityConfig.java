package com.fitian.burntz.global.security.config;

import com.fitian.burntz.domain.auth.oauth2.OAuth2LoginSuccessHandler;
import com.fitian.burntz.domain.auth.oauth2.OAuth2UserServiceImpl;
import com.fitian.burntz.global.security.core.CustomOidcUserService;
import com.fitian.burntz.global.security.core.CustomUserDetailsService;
import com.fitian.burntz.global.security.jwt.JwtTokenFilter;
import com.fitian.burntz.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOidcUserService customOidcUserService;
    private final OAuth2UserServiceImpl oAuth2UserServiceImpl;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtTokenFilter jwtTokenFilter = new JwtTokenFilter(jwtTokenProvider, customUserDetailsService);

        http
                .csrf().disable()
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/login-token-test2.html",
                                "/css/**",
                                "/js/**",
                                "/oauth2/**",
                                "/login/**",
                                "/api/me",
                                "/api/auth/**"
                        )
                        .permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                                .userInfoEndpoint(userInfo -> {
                                    userInfo.userService(oAuth2UserServiceImpl);     // OAuth2 userinfo 처리 (non-OIDC)
                                    userInfo.oidcUserService(customOidcUserService); // OIDC(id_token) 처리 (예: 구글)
                                })
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();

    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
