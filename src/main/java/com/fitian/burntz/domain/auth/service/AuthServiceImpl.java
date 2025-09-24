package com.fitian.burntz.domain.auth.service;

import com.fitian.burntz.domain.auth.dto.AuthTokenResponse;
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
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

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

        MemberCreateResult memberCreateResult;
        try {
            memberCreateResult = oAuthService.findOrCreateUserBySocialToken(socialToken, deviceId, provider);
        } catch (IllegalArgumentException iae) {
            // 예: provider가 없을 때 등 - OAuthService에서 IllegalArgumentException을 던진다면 적절한 ErrorCode로 변환
            throw new ValidationException(ErrorCode.PROVIDER_NOT_FOUND);
        } catch (Exception e) {
            log.error("social login failed", e);
            throw new ValidationException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        Member member = memberCreateResult.member();
        boolean isNewMember = memberCreateResult.isNewMember();

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
        // 검증 책임을 RefreshTokenService로 이동
        RefreshTokenService.ValidationResult validationResult = refreshTokenService.validateRefreshTokenAndDevice(refreshToken, deviceId);

        boolean deleted = refreshTokenService.softDeleteByMemberAndDeviceId(
                validationResult.memberPk(), validationResult.deviceId()
        );

        if (!deleted) {
            // 멱등성 보장: 이미 삭제되었거나 존재하지 않음. 예외 대신 경고 로그 후 정상 종료
            log.warn("logout noop: memberPk={} deviceId={}", validationResult.memberPk(), validationResult.deviceId());
            return;
        }

        if (log.isDebugEnabled()) {
            log.debug("logoutCurrentDevice success memberPk={} deviceId={}", validationResult.memberPk(), validationResult.deviceId());
        }
    }

    @Override
    public void logoutAllDevices(String refreshToken) {
        // refresh 토큰 정보 일치, DB 존재, JWT 검증 수행
        Long memberPk = refreshTokenService.getMemberPkFromValidRefreshToken(refreshToken);

        // soft-deleted 처리
        refreshTokenService.softDeleteAllByMember(memberPk);

        if (log.isDebugEnabled()) {
            log.debug("logoutAllDevices completed memberPk={}", memberPk);
        }
    }

    @Override
    public AuthTokenResponse refreshTokenBased(String refreshToken, String deviceId) {
        RefreshTokenService.ValidationResult validationResult = refreshTokenService.validateRefreshTokenAndDevice(refreshToken, deviceId);

        Member member = memberRepository.findById(validationResult.memberPk())
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        CustomUserDetails principal = new CustomUserDetails(member);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        String newAccessToken = jwtTokenProvider.generateAccessToken(auth, jwtAccessTokenExpirationTime);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(auth, jwtRefreshTokenExpirationTime);

        refreshTokenService.saveOrUpdateRefreshToken(validationResult.memberPk(), newRefreshToken, validationResult.deviceId());

        JwtTokenPair pair = JwtTokenPair.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresIn(jwtAccessTokenExpirationTime / 1000)
                .refreshTokenExpiresIn(jwtRefreshTokenExpirationTime / 1000)
                .build();

        return AuthTokenResponse.builder()
                .jwtTokenPair(pair)
                .memberPk(validationResult.memberPk())
                .deviceId(validationResult.deviceId())
                .build();
        }

    }
