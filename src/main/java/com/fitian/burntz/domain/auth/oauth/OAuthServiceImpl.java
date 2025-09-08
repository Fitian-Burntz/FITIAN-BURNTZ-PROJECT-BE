package com.fitian.burntz.domain.auth.oauth;

import com.fitian.burntz.domain.auth.dto.OAuthUserInfo;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final AppleApiClient appleApiClient;
    private final GoogleApiClient googleApiClient;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public Member findOrCreateUserBySocialToken(String token, String provider) {
        // 기존(token) 기반 흐름은 필요시 유지
        OAuthUserInfo userInfo = switch (provider.toLowerCase()) {
            case "apple" -> appleApiClient.getUserInfoFromIdToken(token);
            case "google" -> googleApiClient.getUserInfo(token);
            default -> throw new IllegalArgumentException("지원하지 않는 provider: " + provider);
        };
        return findOrCreateUserByUserInfo(userInfo, provider);
    }

    @Override
    @Transactional
    public Member findOrCreateUserByUserInfo(OAuthUserInfo userInfo, String provider) {
        if (userInfo == null || userInfo.getMemberId() == null) {
            throw new IllegalArgumentException("Invalid userInfo");
        }
        String providerKey = provider.toLowerCase();
        String providerMemberId = userInfo.getMemberId();

        Optional<Member> existingOpt = memberRepository.findByProviderAndMemberId(providerKey, providerMemberId);
        if (existingOpt.isPresent()) {
            Member existing = existingOpt.get();
            boolean changed = false;
            if (userInfo.getNickname() != null && !userInfo.getNickname().isBlank()
                    && (existing.getNickname() == null || !existing.getNickname().equals(userInfo.getNickname()))) {
                existing.updateProfileIfChanged(userInfo.getNickname(), null, null);
                changed = true;
            }
            if (userInfo.getEmail() != null && !userInfo.getEmail().isBlank()
                    && (existing.getEmail() == null || !existing.getEmail().equals(userInfo.getEmail()))) {
                existing.updateProfileIfChanged(null, userInfo.getEmail(), null);
                changed = true;
            }
            if (changed) memberRepository.save(existing);
            return existing;
        }

        Member newMember = Member.create(
                providerMemberId,
                userInfo.getNickname() != null ? userInfo.getNickname() : "",
                userInfo.getEmail(),
                null,
                providerKey
        );

        try {
            return memberRepository.save(newMember);
        } catch (DataIntegrityViolationException ex) {
            Optional<Member> concurrent = memberRepository.findByProviderAndMemberId(providerKey, providerMemberId);
            if (concurrent.isPresent()) return concurrent.get();
            throw ex;
        }
    }
}
