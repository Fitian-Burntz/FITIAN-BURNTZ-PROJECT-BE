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
            // Java17+: HexFormat; 또는 Commons Codec 사용 가능
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash refresh token", e);
        }
    }

//    @Override
//    @Transactional
//    public void create(Long memberPk, String refreshToken) {
//        Member member = memberRepository.findById(memberPk)
//                .orElseThrow(() -> new IllegalArgumentException("No such member: " + memberPk));
//
//        String hashed = hashToken(refreshToken);
//
//        // 도메인 정적 팩토리 사용 — 직접 setter 사용 없음
//        Auth auth = Auth.createForMember(member, hashed, "default");
//        authRepository.save(auth);
//    }

    @Override
    @Transactional
    public void saveOrUpdateRefreshToken(Long memberPk, String newRefreshToken, String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId is required");
        }
        String did = deviceId.trim();
        String hashed = hashToken(newRefreshToken);

        Auth auth = authRepository.findByMember_MemberPkAndDeviceId(memberPk, did)
                .orElseGet(() -> Auth.createForMember(
                        memberRepository.getReferenceById(memberPk), hashed, did));

        auth.updateRefreshToken(hashed);
        authRepository.save(auth);

        // 개발 디버깅용 로그(원문 X): 저장 여부만 노출
        log.debug("saveOrUpdateRefreshToken: memberPk={} deviceId={} storedHashPresent={}",
                memberPk, did, hashed != null);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public boolean validateRefreshTokenForMember(Long memberPk, String refreshToken) {
        try {
            // 토큰 해시가 일치하는 행이 하나라도 있으면 true (기기 제한 X)
            String incomingHash = hashToken(refreshToken);
            boolean exists = authRepository.existsByMember_MemberPkAndRefreshToken(memberPk, incomingHash);

            // 개발용 로그(원문 X)
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
        // “현재 기기”에서만 쓰고 싶다면 이 메서드는 컨트롤러에서 deviceId를 받아
        // deleteByMemberAndDeviceId 형태로 호출하도록 바꾸는 걸 권장.
        Optional<Auth> opt = authRepository.findTopByMemberMemberPkOrderByAuthPkDesc(memberPk);
        opt.ifPresent(a -> { a.clearRefreshToken(); authRepository.save(a); });
    }


    /**
     * 토큰 기준 삭제 → soft delete 방식으로 구현
     * (DB에서 삭제하지 않고 해당 행의 refreshToken을 제거하고 markDeleted() 호출 후 저장)
     */
    @Override
    @Transactional
    public boolean softDeleteByMemberAndToken(Long memberPk, String refreshToken) {
        String hash = hashToken(refreshToken);

        // 조회
        List<Auth> list = authRepository.findAllByMember_MemberPkAndRefreshToken(memberPk, hash);
        if (list == null || list.isEmpty()) {
            log.debug("deleteByMemberAndToken memberPk={} found=0", memberPk);
            return false;
        }

        // soft-delete 처리: refreshToken 제거 + markDeleted() (옵션)
        for (Auth a : list) {
            a.clearRefreshToken();   // sets refreshToken = null and updates updatedAt
            a.markDeleted();         // optional: depends on BaseTime impl
            authRepository.save(a);
        }

        log.debug("deleteByMemberAndToken memberPk={} affected={}", memberPk, list.size());
        return true;
    }

    /**
     * 모든 기기 삭제 (soft delete)
     */
    @Override
    @Transactional
    public void softDeleteAllByMember(Long memberPk) {
        List<Auth> list = authRepository.findAllByMember_MemberPk(memberPk);
        if (list == null || list.isEmpty()) {
            log.debug("deleteAllByMember memberPk={} found=0", memberPk);
            return;
        }

        for (Auth a : list) {
            a.clearRefreshToken();
            a.markDeleted(); // optional
            authRepository.save(a);
        }

        log.debug("deleteAllByMember memberPk={} affected={}", memberPk, list.size());
    }

    /**
     * 기기 기준 삭제 (soft delete)
     */
    @Override
    @Transactional
    public boolean softDeleteByMemberAndDeviceId(Long memberPk, String deviceId) { // [CHANGED -> soft delete]
        if (deviceId == null || deviceId.isBlank()) return false;
        String did = deviceId.trim();

        Optional<Auth> opt = authRepository.findByMember_MemberPkAndDeviceId(memberPk, did);
        if (opt.isEmpty()) {
            log.debug("deleteByMemberAndDeviceId memberPk={} deviceId={} found=false", memberPk, did);
            return false;
        }

        Auth auth = opt.get();
        auth.clearRefreshToken();
        auth.markDeleted(); // optional
        authRepository.save(auth);

        log.debug("deleteByMemberAndDeviceId memberPk={} deviceId={} softDeleted=true", memberPk, did);
        return true;
    }

}