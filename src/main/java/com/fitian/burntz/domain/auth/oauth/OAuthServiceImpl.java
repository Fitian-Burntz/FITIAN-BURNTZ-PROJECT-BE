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

        // 변경: 직접 memberRepository로 먼저 조회하지 않고 getOrCreate 호출만 함
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