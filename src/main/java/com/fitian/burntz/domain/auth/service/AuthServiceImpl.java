package com.fitian.burntz.domain.auth.service;

import com.fitian.burntz.domain.auth.entity.Auth;
import com.fitian.burntz.domain.auth.repository.AuthRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.common.util.HashUtil;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthRepository authRepository;
    private final MemberRepository memberRepository;
    private final SecretKey secretKey; // JwtKey에서 제공하는 SecretKey 빈 주입

    // ----------------- 기존 logout 로직 유지 + DB 무효화 추가 -----------------
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        // 1) 서버 세션 무효화 (있다면)
        try {
            var session = request.getSession(false);
            if (session != null) {
                session.invalidate();
                log.info("[AuthService] invalidated session");
            }
        } catch (Exception ex) {
            log.warn("[AuthService] session invalidate failed: {}", ex.getMessage());
        }

        // 2) 스프링 시큐리티 컨텍스트 클리어
        // 단, 로그아웃 전에 현재 사용자 식별을 위해 Authentication 확인
        Long memberPk = null;
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails) {
                CustomUserDetails cud = (CustomUserDetails) authentication.getPrincipal();
                memberPk = cud.getMemberPk();
            }
        } catch (Exception e) {
            log.debug("[AuthService] unable to read principal for revoke: {}", e.getMessage());
        }

        SecurityContextHolder.clearContext();
        log.info("[AuthService] cleared SecurityContext");

        // 3) 쿠키 만료
        expireCookie(response, "accessToken", "/");
        expireCookie(response, "refreshToken", "/");
        expireCookie(response, "JSESSIONID", "/");

        // 4) DB에 저장된 refresh token 무효화 (deviceId 우선, 없으면 전체 삭제)
        String deviceId = readCookie(request, "deviceId");
        try {
            if (memberPk != null) {
                if (deviceId != null) {
                    revokeRefreshToken(memberPk, deviceId);
                } else {
                    revokeAllRefreshTokensForMember(memberPk);
                }
            } else {
                // memberPk를 쿠키에서 파싱해볼 수도 있음 (선택적)
                String memberPkStr = readCookie(request, "memberPk");
                if (memberPkStr != null) {
                    try {
                        Long parsed = Long.valueOf(memberPkStr);
                        if (deviceId != null) revokeRefreshToken(parsed, deviceId);
                        else revokeAllRefreshTokensForMember(parsed);
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {
            log.warn("[AuthService] revoke refresh token failed: {}", e.getMessage());
        }
    }

    // ----------------- refresh token 저장/검증/무효화 -----------------
    @Override
    @Transactional
    public void saveOrUpdateRefreshToken(Long memberPk, String rawRefreshToken, String deviceId) {
        Member member = memberRepository.findById(memberPk)
                .orElseThrow(() -> new IllegalArgumentException("No member: " + memberPk));

        String hashed = HashUtil.hmacSha512Hex(rawRefreshToken, secretKey.getEncoded());

        Optional<Auth> opt;
        if (deviceId != null && !deviceId.isBlank()) {
            opt = authRepository.findByDeviceIdAndMember(deviceId, member);
        } else {
            opt = authRepository.findByMember(member);
        }

        if (opt.isPresent()) {
            Auth auth = opt.get();
            auth.updateRefreshToken(hashed);
            log.info("[AuthService] updated refresh token for memberPk={} deviceId={}", memberPk, deviceId);
        } else {
            Auth auth = Auth.builder()
                    .member(member)
                    .deviceId(deviceId)
                    .refreshToken(hashed)
                    .build();
            try {
                authRepository.save(auth);
                log.info("[AuthService] created refresh token record for memberPk={} deviceId={}", memberPk, deviceId);
            } catch (DataIntegrityViolationException ex) {
                log.warn("[AuthService] concurrent insert detected, re-querying for memberPk={} deviceId={}", memberPk, deviceId);
                Optional<Auth> retry = (deviceId != null && !deviceId.isBlank())
                        ? authRepository.findByDeviceIdAndMember(deviceId, member)
                        : authRepository.findByMember(member);
                if (retry.isPresent()) {
                    retry.get().updateRefreshToken(hashed);
                } else {
                    throw ex;
                }
            }
        }
    }

    @Override
    public boolean validateRefreshTokenForMember(Long memberPk, String rawRefreshToken) {
        Member member = memberRepository.findById(memberPk)
                .orElseThrow(() -> new IllegalArgumentException("No member: " + memberPk));

        return authRepository.findByMember(member)
                .map(a -> HashUtil.matchesHmacSha512(rawRefreshToken, a.getRefreshToken(), secretKey.getEncoded()))
                .orElse(false);
    }

    @Override
    @Transactional
    public void revokeRefreshToken(Long memberPk, String deviceId) {
        Member member = memberRepository.findById(memberPk)
                .orElseThrow(() -> new IllegalArgumentException("No member: " + memberPk));
        if (deviceId != null && !deviceId.isBlank()) {
            authRepository.findByDeviceIdAndMember(deviceId, member)
                    .ifPresent(authRepository::delete);
            log.info("[AuthService] revoked refresh token for memberPk={} deviceId={}", memberPk, deviceId);
        } else {
            authRepository.deleteByMember(member);
            log.info("[AuthService] revoked ALL refresh tokens for memberPk={} (no deviceId provided)", memberPk);
        }
    }

    @Override
    @Transactional
    public void revokeAllRefreshTokensForMember(Long memberPk) {
        Member member = memberRepository.findById(memberPk)
                .orElseThrow(() -> new IllegalArgumentException("No member: " + memberPk));
        authRepository.deleteByMember(member);
        log.info("[AuthService] revoked ALL refresh tokens for memberPk={}", memberPk);
    }

    // ----------------- helper -----------------
    private void expireCookie(HttpServletResponse response, String name, String path) {
        Cookie cookie = new Cookie(name, "");
        cookie.setPath(path);
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        response.addHeader("Set-Cookie", name + "=; Path=" + path + "; Max-Age=0; HttpOnly; SameSite=Lax");
        log.info("[AuthService] expired cookie: {}", name);
    }

    private String readCookie(HttpServletRequest request, String name) {
        if (request == null || request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}
