package com.fitian.burntz.domain.auth.service;

import com.fitian.burntz.domain.auth.repository.AuthRepository;
import com.fitian.burntz.global.common.util.PreconditionValidator;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final AuthRepository authRepository;
    private final JwtTokenProvider jwtTokenProvider; // 토큰 검증용 주입
    private final PreconditionValidator preconditionValidator;

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash refresh token", e);
        }
    }

    @Override
    @Transactional
    public void saveOrUpdateRefreshToken(Long memberPk, String newRefreshToken, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId is required");
        }
        String did = deviceId.trim();
        String hashed = hashToken(newRefreshToken);

        // 원자적으로 INSERT or UPDATE 처리 (Postgres)
        authRepository.upsertAuth(memberPk, did, hashed);

        log.debug("saveOrUpdateRefreshToken: memberPk={} deviceId={} upsertDone", memberPk, did);
    }

    @Override
    @Transactional
    public void softDeleteAllByMember(Long memberPk) {
        int affected = authRepository.softDeleteAllByMemberPkNative(memberPk);
        log.debug("deleteAllByMember memberPk={} affected={}", memberPk, affected);
    }

    @Override
    @Transactional
    public boolean softDeleteByMemberAndDeviceId(Long memberPk, String deviceId) {
        String targetDeviceId = preconditionValidator.requireDeviceId(deviceId);

        if (targetDeviceId == null){
            return false;
        }

        int affected = authRepository.softDeleteByMemberPkAndDeviceIdNative(memberPk, targetDeviceId);
        log.debug("deleteByMemberAndDeviceId memberPk={} deviceId={} affected={}", memberPk, targetDeviceId, affected);

        return affected > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshTokenService.ValidationResult validateRefreshTokenAndDevice(String refreshToken, String deviceId) throws ValidationException {
        Long memberPk = getMemberPkFromValidRefreshToken(refreshToken);

        // deviceId 정제 및 null 체크
        String targetDeviceId = preconditionValidator.requireDeviceId(deviceId);
        return new RefreshTokenService.ValidationResult(memberPk, targetDeviceId);
    }

    /** refreshToken 여부 및 JWT 검증 (더 세밀한 검증 수행) **/
    @Override
    @Transactional(readOnly = true)
    public Long getMemberPkFromValidRefreshToken(String refreshToken) throws ValidationException {
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
        if (!isRefreshTokenValidForMember(memberPk, refreshToken)) {
            throw new ValidationException(ErrorCode.TOKEN_INVALID);
        }
        return memberPk;
    }


    /** refreshToken 의 멤버 일치 & DB 존재 여부만 확인 **/
    private boolean isRefreshTokenValidForMember(Long memberPk, String refreshToken) {
        if (memberPk == null || refreshToken == null || refreshToken.isBlank()) {
            return false;
        }
        try {
            String incomingHash = hashToken(refreshToken);
            boolean exists = authRepository.existsByMember_MemberPkAndRefreshToken(memberPk, incomingHash);
            log.debug("validateRefreshTokenForMember: memberPk={} storedHashMatch={}", memberPk, exists);
            return exists;
        } catch (DataAccessException dae) {
            // 로그는 남기되 예외는 다시 던져서 5xx로 DB 에러 노출
            log.error("DB error during refresh-token validation for memberPk={}", memberPk, dae);
            throw dae;
        } catch (Exception e) {
            log.error("Unexpected error during refresh-token validation for memberPk={}", memberPk, e);
            throw new RuntimeException("Failed to validate refresh token", e);
        }
    }
}
