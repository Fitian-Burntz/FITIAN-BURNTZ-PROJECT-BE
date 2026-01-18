package com.fitian.burntz.global.security.config;

import com.fitian.burntz.global.security.core.CustomUserDetailsService;
import com.fitian.burntz.global.security.filter.InternalPushAuthFilter;
import com.fitian.burntz.global.security.handler.RestAccessDeniedHandler;
import com.fitian.burntz.global.security.handler.RestAuthenticationEntryPoint;
import com.fitian.burntz.global.security.jwt.JwtTokenFilter;
import com.fitian.burntz.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;
    private final InternalPushAuthFilter internalPushAuthFilter;

    @Bean
    @Order(0)
    public SecurityFilterChain internalPushChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/v1/alarm/push-message")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // 기준은 "등록된 표준 필터"로 잡는다
                .addFilterBefore(internalPushAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtTokenFilter jwtTokenFilter = new JwtTokenFilter(jwtTokenProvider, customUserDetailsService);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/login-token-test3.html",
                                "/css/**",
                                "/js/**",
                                //결제 테스트용
                                "/api/v1/payments/**",
                                //ECS 헬스체크
                                "/actuator/health", "/actuator/health/**",
                                //wod(임시로 permitAll)
                                "/api/v1/boxes/**",

                                // 스웨거
                                "/swagger-ui/**",
                                "/v3/api-docs/**",

                                // 어드민 페이지
                                "/api/v1/admin/**",
                                "/admin/**"
                        ).permitAll()


                        //== Auth 무인증 경로 (검증이 필요할 경우 토큰 검증으로 확인)  ==/
                        .requestMatchers(
                                HttpMethod.POST, "/api/v1/auth/**"
                        ).permitAll()

                        //== Box 무인증 경로 ==//
                        .requestMatchers(
                               HttpMethod.GET, "/api/v1/boxes/all"
                        ).permitAll()

                        //firebase push
                        .requestMatchers(HttpMethod.POST, "/api/v1/alarm/push-message").permitAll()
                        .anyRequest().authenticated()
                )
                // OAuth2 웹로그인을 사용하지 않으므로 oauth2Login() 설정 제거
                .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // CORS 정책을 Security가 사용할 수 있게 빈으로 등록
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // 실제 서비스 도메인만 허용하세요. 개발/테스트시 임시로 "*" 사용 가능하지만 보안상 주의.
        config.setAllowedOrigins(List.of("https://server.burntz.app"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
