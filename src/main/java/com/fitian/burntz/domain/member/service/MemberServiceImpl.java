package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.member.dto.MemberCreateResult;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.member_enum.Gender;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    /**
     * provider + memberId 조합으로 조회 후 없으면 생성. 동시성 발생 시 DataIntegrityViolationException을 잡아 재조회하여 정상화.
     */
    @Override
    @Transactional
    public MemberCreateResult getOrCreateMember(String provider, String memberId, String nickname, String email) {
        Optional<Member> existing = memberRepository.findByProviderAndMemberId(provider, memberId);
        if (existing.isPresent()) {
            return new MemberCreateResult(existing.get(), false);
        }

        // 기존 동작을 유지: nickname, email이 null이면 fallback 값 사용
        String safeNickname = (nickname == null || nickname.isBlank()) ? "nickname_unset" : nickname;
        String safeEmail = (email == null || email.isBlank()) ? "email_unset" : email;

        // 정적 팩토리 메서드 사용
        Member newMember = Member.create(memberId, safeNickname, safeEmail, provider);

        try {
            Member saved = memberRepository.save(newMember);
            return new MemberCreateResult(saved, true);
        } catch (DataIntegrityViolationException dive) {
            // 동시성으로 다른 트랜잭션이 생성했을 가능성 -> 재조회
            Member saved = memberRepository.findByProviderAndMemberId(provider, memberId)
                    .orElseThrow(() -> dive); // 예외 재던지기
            return new MemberCreateResult(saved, false);
        }
    }
}