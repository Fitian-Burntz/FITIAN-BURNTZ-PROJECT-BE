package com.fitian.burntz.domain.auth.oauth;

import com.fitian.burntz.domain.auth.dto.OAuthUserInfo;
import com.fitian.burntz.domain.member.dto.MemberCreateResult;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.domain.member.service.MemberService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final AppleApiClient appleApiClient;
    private final GoogleApiClient googleApiClient;
    private final MemberService memberService;
    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public MemberCreateResult findOrCreateUserBySocialToken(String token, String deviceId, String provider) {
        OAuthUserInfo userInfo = switch (provider.toLowerCase()) {
            case "apple" -> appleApiClient.getUserInfoFromIdToken(token);
            case "google" -> googleApiClient.getUserInfo(token);
            default -> throw new IllegalArgumentException("지원하지 않는 provider: " + provider);
        };
        return findOrCreateUserByUserInfo(userInfo, deviceId, provider);
    }

    @Override
    @Transactional
    public MemberCreateResult findOrCreateUserByUserInfo(OAuthUserInfo userInfo, String deviceId, String provider) {
        if (userInfo == null || userInfo.getMemberId() == null) {
            throw new IllegalArgumentException("Invalid userInfo");
        }
        String providerKey = provider.toLowerCase();
        String providerMemberId = userInfo.getMemberId();

        //먼저 DB에서 조회 — 이미 존재하면 생성 없이 반환 (안전)
        Optional<Member> existingAccount = memberRepository.findByProviderAndMemberId(providerKey, providerMemberId);
        if (existingAccount.isPresent()) {
            return new MemberCreateResult(existingAccount.get(), false);
        }

        //  존재하지 않으면 생성 시도 — 이때 deviceId 반드시 필요
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId is required for member creation");
        }

        // memberService에 위임 — MemberCreateResult 반환(생성 여부 포함)
        MemberCreateResult createResult = memberService.getOrCreateMember(
                providerKey,
                providerMemberId,
                userInfo.getNickname() != null ? userInfo.getNickname() : "",
                userInfo.getEmail()
        );

        Member member = createResult.member();
        boolean isNew = createResult.isNewMember();

        // 기존 로직: 기존 사용자라면 프로필 갱신 처리
        if (!isNew) {
            boolean changed = false;
            if (userInfo.getNickname() != null && !userInfo.getNickname().isBlank()
                    && (member.getNickname() == null || !member.getNickname().equals(userInfo.getNickname()))) {
                member.updateProfileIfChanged(userInfo.getNickname(), null, null);
                changed = true;
            }
            if (userInfo.getEmail() != null && !userInfo.getEmail().isBlank()
                    && (member.getEmail() == null || !member.getEmail().equals(userInfo.getEmail()))) {
                member.updateProfileIfChanged(null, userInfo.getEmail(), null);
                changed = true;
            }
            if (changed) {
                memberRepository.save(member);
            }
        }

        return createResult;
    }
}