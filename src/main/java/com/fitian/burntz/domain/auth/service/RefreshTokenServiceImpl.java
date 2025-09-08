package com.fitian.burntz.domain.auth.service;

import com.fitian.burntz.domain.auth.entity.Auth;
import com.fitian.burntz.domain.auth.repository.AuthRepository;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
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

    @Override
    @Transactional
    public void create(Long memberPk, String refreshToken) {
        Member member = memberRepository.findById(memberPk)
                .orElseThrow(() -> new IllegalArgumentException("No such member: " + memberPk));

        String hashed = hashToken(refreshToken);

        // 도메인 정적 팩토리 사용 — 직접 setter 사용 없음
        Auth auth = Auth.createForMember(member, hashed, "default");
        authRepository.save(auth);
    }

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
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public boolean validateRefreshTokenForMember(Long memberPk, String refreshToken) {
        // 토큰 해시가 일치하는 행이 하나라도 있으면 true (기기 제한 X)
        String incomingHash = hashToken(refreshToken);
        return authRepository.existsByMember_MemberPkAndRefreshToken(memberPk, incomingHash);
    }

    @Override
    @Transactional
    public void revokeRefreshToken(Long memberPk) {
        // “현재 기기”에서만 쓰고 싶다면 이 메서드는 컨트롤러에서 deviceId를 받아
        // deleteByMemberAndDeviceId 형태로 호출하도록 바꾸는 걸 권장.
        Optional<Auth> opt = authRepository.findTopByMemberMemberPkOrderByAuthPkDesc(memberPk);
        opt.ifPresent(a -> { a.clearRefreshToken(); authRepository.save(a); });
    }


    @Override
    @Transactional
    public boolean deleteByMemberAndToken(Long memberPk, String refreshToken) {
        String hash = hashToken(refreshToken);
        int affected = authRepository.deleteByMemberMemberPkAndRefreshToken(memberPk, hash);
        return affected > 0;
    }

    @Override
    @Transactional
    public void deleteAllByMember(Long memberPk) {
        authRepository.deleteByMemberMemberPk(memberPk);

    }

    @Override
    @Transactional
    public boolean deleteByMemberAndDeviceId(Long memberPk, String deviceId) { // [ADDED]
        int affected = authRepository.deleteByMemberMemberPkAndDeviceId(memberPk, deviceId.trim());
        return affected > 0;
    }

}