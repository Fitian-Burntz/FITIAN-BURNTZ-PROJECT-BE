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
@Slf4j
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public MemberCreateResult getOrCreateMember(String provider, String memberId, String name, String email) {
        // 방어적 정규화
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(memberId, "memberId must not be null");

        provider = provider.trim().toLowerCase(Locale.ROOT);
        memberId = memberId.trim();

        // name은 외부값을 사용하지 않고 항상 기본값으로 설정
        name = "nickname_unset";

        email = (email == null) ? "" : email;

        // 1) 조회
        Optional<Member> opt = memberRepository.findByProviderAndMemberId(provider, memberId);
        if (opt.isPresent()) {
            log.debug("[MemberService] existing member found provider={} memberId={}", provider, memberId);
            return new MemberCreateResult(opt.get(), false);
        }

        // 2) 새 멤버 생성 — Member.create 내부에서 gender 기본값 설정됨
        Member newMember = Member.create(memberId, name, email, provider);

        try {
            // saveAndFlush로 즉시 PK 확보
            Member saved = memberRepository.saveAndFlush(newMember);
            log.info("[MemberProvision] created member pk={} provider={} memberId={}", saved.getMemberPk(), provider, memberId);

            // 생성 후 후처리 훅(필요시 구현)
            afterMemberCreated(saved);

            return new MemberCreateResult(saved, true);
        } catch (DataIntegrityViolationException ex) {
            // 동시성: 누군가 먼저 만들었을 가능성 -> 재조회
            log.warn("[MemberProvision] concurrent insert detected for {}/{}. re-querying...", provider, memberId);
            return memberRepository.findByProviderAndMemberId(provider, memberId)
                    .map(m -> new MemberCreateResult(m, false))
                    .orElseThrow(() -> new RuntimeException("Failed to create member and no existing member found", ex));
        }
    }

    private void afterMemberCreated(Member saved) {
        // 온보딩/이벤트/초기 설정 등 (비동기 처리 권장)
    }
}