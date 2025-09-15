package com.fitian.burntz.domain.auth.service;

import com.fitian.burntz.domain.auth.entity.Auth;
import com.fitian.burntz.domain.auth.repository.AuthRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import com.fitian.burntz.global.security.jwt.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final AuthRepository authRepository;
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider; // 토큰 검증용 주입

    /**
     * 단순 SHA-256 해시 (운영에서는 추가적인 솔트/HMAC 또는 KMS 암호화 권장)
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash refresh token", e);
        }
    }

    /**
     * 기존 find -> create -> save 흐름을 네이티브 upsert로 변경해서 DB 왕복 수를 1회로 줄임.
     */
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

        log.debug("saveOrUpdateRefreshToken: memberPk={} deviceId={} storedHashPresent={}",
                memberPk, did, hashed != null);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public boolean validateRefreshTokenForMember(Long memberPk, String refreshToken) {
        try {
            String incomingHash = hashToken(refreshToken);
            boolean exists = authRepository.existsByMember_MemberPkAndRefreshToken(memberPk, incomingHash);
            log.debug("validateRefreshTokenForMember: memberPk={} storedHashMatch={}", memberPk, exists);
            return exists;
        } catch (Exception e) {
            log.error("validateRefreshTokenForMember ERROR memberPk={} error={}", memberPk, e.getMessage(), e);
            return false;
        }
    }

    @Override
    @Transactional
    public void revokeRefreshToken(Long memberPk) {
        Optional<Auth> opt = authRepository.findTopByMemberMemberPkOrderByAuthPkDesc(memberPk);
        opt.ifPresent(a -> {
            a.clearRefreshToken();
            authRepository.save(a);
        });
    }

    /**
     * 토큰 기준 삭제 (soft delete) — 엔티티 루프 사용
     */
    @Override
    @Transactional
    public boolean softDeleteByMemberAndToken(Long memberPk, String refreshToken) {
        String hash = hashToken(refreshToken);
        List<Auth> list = authRepository.findAllByMember_MemberPkAndRefreshToken(memberPk, hash);
        if (list == null || list.isEmpty()) {
            log.debug("deleteByMemberAndToken memberPk={} found=0", memberPk);
            return false;
        }

        for (Auth a : list) {
            a.clearRefreshToken();
            a.markDeleted();
            authRepository.save(a);
        }

        log.debug("deleteByMemberAndToken memberPk={} affected={}", memberPk, list.size());
        return true;
    }

    /**
     * 모든 기기 삭제 (soft delete) — bulk native update 사용
     */
    @Override
    @Transactional
    public void softDeleteAllByMember(Long memberPk) {
        int affected = authRepository.softDeleteAllByMemberPkNative(memberPk);
        log.debug("deleteAllByMember memberPk={} affected={}", memberPk, affected);
    }

    /**
     * 기기 기준 삭제 (soft delete) — native bulk update 사용
     */
    @Override
    @Transactional
    public boolean softDeleteByMemberAndDeviceId(Long memberPk, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) return false;
        String did = deviceId.trim();

        int affected = authRepository.softDeleteByMemberPkAndDeviceIdNative(memberPk, did);
        log.debug("deleteByMemberAndDeviceId memberPk={} deviceId={} affected={}", memberPk, did, affected);
        return affected > 0;
    }

    // ------------------- 여기서 토큰 검증 책임을 집중 -------------------

    /**
     * AuthServiceImpl에서 사용하던 헬퍼(토큰 원문 검증 + DB 일치 검증)를 여기로 옮겼습니다.
     * - 클레임 기반으로 리프레시 토큰인지 확인 (jwtTokenProvider.isRefreshToken)
     * - 서명/만료 등 기본 검증 (jwtTokenProvider.validateToken)
     * - 토큰에서 memberPk 추출 (jwtTokenProvider.getMemberPkFromRefreshToken)
     * - DB 저장 해시와 일치하는지 검사 (validateRefreshTokenForMember)
     *
     * ValidationException(ErrorCode.*) 을 던져 호출자가 통일된 예외 정책을 받도록 합니다.
     */
    @Override
    public RefreshTokenService.ValidationResult validateRefreshTokenAndDevice(String refreshToken, String deviceId) throws ValidationException {
        Long memberPk = getMemberPkFromValidRefreshToken(refreshToken);

        if (deviceId == null || deviceId.isBlank()) {
            throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        return new RefreshTokenService.ValidationResult(memberPk, deviceId.trim());
    }

    /**
     * private helper: refreshToken 원문 검증 및 DB 일치 확인
     */
    @Transactional(Transactional.TxType.SUPPORTS)
    private Long getMemberPkFromValidRefreshToken(String refreshToken) throws ValidationException {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ValidationException(ErrorCode.TOKEN_EXTRACTION_FAILED);
        }
        // 클레임 기반 검사: token_type 등 확인
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new ValidationException(ErrorCode.TOKEN_INVALID);
        }
        // 서명/만료 검사
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new ValidationException(ErrorCode.TOKEN_INVALID);
        }
        Long memberPk = jwtTokenProvider.getMemberPkFromRefreshToken(refreshToken);
        if (memberPk == null) {
            throw new ValidationException(ErrorCode.TOKEN_INVALID);
        }
        // DB에 저장된 해시와 일치하는지 검사
        if (!validateRefreshTokenForMember(memberPk, refreshToken)) {
            throw new ValidationException(ErrorCode.TOKEN_INVALID);
        }
        return memberPk;
    }
}
