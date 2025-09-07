package com.fitian.burntz.domain.member.service;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    /**
     * provider/memberId 는 서비스 호출 이전에 정규화되어 들어오는 것을 권장.
     * (하지만 이 메서드 내에서도 간단한 정규화/검사 수행)
     */
    @Override
    @Transactional
    public Member getOrCreateMember(String provider, String memberId, String name, String email) {
        // 방어 코드: null -> 빈 문자열 또는 소문자 정규화
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(memberId, "memberId must not be null");

        provider = provider.trim().toLowerCase(Locale.ROOT);
        memberId = memberId.trim();

        // 먼저 조회
        var opt = memberRepository.findByProviderAndMemberId(provider, memberId);
        if (opt.isPresent()) {
            return opt.get();
        }

        // 새 멤버 준비
        Member newMember = Member.create(
                memberId,
                (name != null && !name.isBlank()) ? name : "socialUser",
                (email != null) ? email : "",
                Gender.OTHERS,
                provider
        );


        try {
            Member saved = memberRepository.save(newMember);
            log.info("[MemberProvision] created member pk={} provider={} memberId={}", saved.getMemberPk(), provider, memberId);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            // 동시성으로 다른 스레드가 먼저 insert 했을 가능성 -> 재조회
            log.warn("[MemberProvision] concurrent insert detected for {}/{}. re-querying...", provider, memberId);
            return memberRepository.findByProviderAndMemberId(provider, memberId)
                    .orElseThrow(() -> new RuntimeException("Failed to create member and no existing member found", ex));
        }
    }
}
