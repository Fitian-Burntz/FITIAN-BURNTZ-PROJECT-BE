package com.fitian.burntz.domain.auth.service;

import com.fitian.burntz.domain.auth.dto.JwtTokenPair;
import com.fitian.burntz.domain.auth.dto.LoginResponse;
import com.fitian.burntz.domain.member.dto.MemberCreateResult;
import com.fitian.burntz.domain.member.dto.MemberDto;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.common.util.SecureLogUtil;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import com.fitian.burntz.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService{

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberRepository memberRepository;
    private final com.fitian.burntz.domain.auth.oauth.OAuthService oAuthService;
    private final RefreshTokenService refreshTokenService;

    @Value("${jwt.accessTokenExpirationTime}")
    private Long jwtAccessTokenExpirationTime;

    @Value("${jwt.refreshTokenExpirationTime}")
    private Long jwtRefreshTokenExpirationTime;

    @Override
    public LoginResponse loginWithSocial(String socialToken, String provider, String deviceId) {
        if (socialToken == null || socialToken.isBlank()) {
            throw new ValidationException(ErrorCode.TOKEN_EXTRACTION_FAILED);
        }
        if (deviceId == null || deviceId.isBlank()) {
            throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        deviceId = deviceId.trim();

        if (log.isDebugEnabled()) {
            log.debug("loginWithSocial called provider={} tokenHashPrefix={} deviceId={}",
                    provider,
                    SecureLogUtil.sha256Prefix(socialToken, 8),
                    deviceId);
        }

        MemberCreateResult createResult;
        try {
            createResult = oAuthService.findOrCreateUserBySocialToken(socialToken, deviceId, provider);
        } catch (IllegalArgumentException iae) {
            // 예: provider가 없을 때 등 - OAuthService에서 IllegalArgumentException을 던진다면 적절한 ErrorCode로 변환
            throw new ValidationException(ErrorCode.PROVIDER_NOT_FOUND);
        } catch (Exception e) {
            log.error("social login failed", e);
            throw new ValidationException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        Member member = createResult.member();
        boolean isNewMember = createResult.isNewMember();

        JwtTokenPair pair = jwtTokenProvider.createTokenPair(member);
        refreshTokenService.saveOrUpdateRefreshToken(member.getMemberPk(), pair.getRefreshToken(), deviceId);

        return LoginResponse.builder()
                .jwtTokenPair(pair)
                .member(MemberDto.from(member))
                .newMember(isNewMember)
                .deviceId(deviceId)
                .build();
    }

    @Override
    public void logoutCurrentDevice(String refreshToken, String deviceId) {
        ValidationResult vr = validateRefreshTokenAndDeviceId(refreshToken, deviceId);

        boolean deleted = refreshTokenService.softDeleteByMemberAndDeviceId(vr.memberPk(), vr.deviceId());
        if (!deleted) {
            // 기존 핸들러로 404 처리되도록 ValidationException 사용
            throw new ValidationException(ErrorCode.DEVICE_NOT_FOUND);
        }

        if (log.isDebugEnabled()) {
            log.debug("logoutCurrentDevice success memberPk={} deviceId={}", vr.memberPk(), vr.deviceId());
        }
    }

    @Override
    public void logoutAllDevices(String anyToken) {
        if (anyToken == null || anyToken.isBlank()) {
            throw new ValidationException(ErrorCode.TOKEN_EXTRACTION_FAILED);
        }
        if (!jwtTokenProvider.validateToken(anyToken)) {
            throw new ValidationException(ErrorCode.TOKEN_INVALID);
        }

        Long memberPk = jwtTokenProvider.getMemberPkFromToken(anyToken);
        if (memberPk == null) {
            throw new ValidationException(ErrorCode.TOKEN_INVALID);
        }

        refreshTokenService.softDeleteAllByMember(memberPk);

        if (log.isDebugEnabled()) {
            log.debug("logoutAllDevices completed memberPk={}", memberPk);
        }
    }

    @Override
    public Map<String, Object> refreshTokenBased(String refreshToken, String deviceId) {
        ValidationResult vr = validateRefreshTokenAndDeviceId(refreshToken, deviceId);

        Member member = memberRepository.findById(vr.memberPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        CustomUserDetails principal = new CustomUserDetails(member);
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String newAccessToken = jwtTokenProvider.generateAccessToken(auth, jwtAccessTokenExpirationTime);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(auth, jwtRefreshTokenExpirationTime);

        refreshTokenService.saveOrUpdateRefreshToken(vr.memberPk(), newRefreshToken, vr.deviceId());

        if (log.isDebugEnabled()) {
            log.debug("refreshTokenBased success memberPk={} deviceId={} newRefreshHashPrefix={}",
                    vr.memberPk(), vr.deviceId(), SecureLogUtil.sha256Prefix(newRefreshToken, 8));
        }

        return Map.of(
                "accessToken", newAccessToken,
                "accessTokenExpiresIn", jwtAccessTokenExpirationTime / 1000,
                "refreshToken", newRefreshToken,
                "refreshTokenExpiresIn", jwtRefreshTokenExpirationTime / 1000,
                "memberPk", vr.memberPk(),
                "deviceId", vr.deviceId()
        );
    }

    // ---------- private helpers ----------
    private Long requireValidRefreshTokenAndGetMemberPk(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ValidationException(ErrorCode.TOKEN_EXTRACTION_FAILED);
        }
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new ValidationException(ErrorCode.TOKEN_INVALID);
        }
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new ValidationException(ErrorCode.TOKEN_INVALID);
        }
        Long memberPk = jwtTokenProvider.getMemberPkFromRefreshToken(refreshToken);
        if (memberPk == null) {
            throw new ValidationException(ErrorCode.TOKEN_INVALID);
        }
        if (!refreshTokenService.validateRefreshTokenForMember(memberPk, refreshToken)) {
            throw new ValidationException(ErrorCode.TOKEN_INVALID);
        }
        return memberPk;
    }

    private ValidationResult validateRefreshTokenAndDeviceId(String refreshToken, String deviceId) {
        Long memberPk = requireValidRefreshTokenAndGetMemberPk(refreshToken);
        if (deviceId == null || deviceId.isBlank()) {
            throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        return new ValidationResult(memberPk, deviceId.trim());
    }

    private static record ValidationResult(Long memberPk, String deviceId) {}
}