package com.fitian.burntz.domain.auth.service;

import com.fitian.burntz.domain.auth.entity.Auth;
import com.fitian.burntz.domain.auth.repository.AuthRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * 토큰 기준 삭제 (soft delete) — bulk native update로 변경
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

        // 기존 개별 save 대신 bulk update 권장(여기선 엔티티 접근 유지)
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
}