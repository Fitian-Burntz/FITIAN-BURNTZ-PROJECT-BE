package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.member.dto.MemberCreateResponse;
import com.fitian.burntz.domain.member.dto.MemberDto;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.member_enum.Gender;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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
    public MemberCreateResponse getOrCreateMember(String provider, String memberId, String nickname, String email) {
        Optional<Member> existing = memberRepository.findByProviderAndMemberId(provider, memberId);
        if (existing.isPresent()) {
            return new MemberCreateResponse(existing.get(), false);
        }

        // 기존 동작을 유지: nickname, email이 null이면 fallback 값 사용
        String safeNickname = (nickname == null || nickname.isBlank()) ? "nickname_unset" : nickname;
        String safeEmail = (email == null || email.isBlank()) ? "email_unset" : email;

        // 정적 팩토리 메서드 사용
        Member newMember = Member.create(memberId, safeNickname, safeEmail, provider);

        try {
            Member saved = memberRepository.save(newMember);
            return new MemberCreateResponse(saved, true);
        } catch (DataIntegrityViolationException dive) {
            // 동시성으로 다른 트랜잭션이 생성했을 가능성 -> 재조회
            Member saved = memberRepository.findByProviderAndMemberId(provider, memberId)
                    .orElseThrow(() -> dive); // 예외 재던지기
            return new MemberCreateResponse(saved, false);
        }
    }

    /**
     * 컨트롤러에서 memberPk를 직접 전달하는 버전 (권장)
     */

    @Override
    @Transactional
    public MemberDto updateMemberInfo(Long memberPk, String newNickname, String newGender) {
        if (memberPk == null) {
            throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        Member member = memberRepository.findById(memberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        boolean changed = false;

        // 닉네임 처리 (null이면 변경 안함)
        if (newNickname != null) {
            String trimmed = newNickname.trim();
            if (trimmed.isEmpty()) {
                throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
            }

            if (!trimmed.equals(member.getNickname())) {
                // 중복 허용: 더 이상 existsByNickname 체크하지 않음
                member.updateMemberProfile(trimmed, null, null);
                changed = true;
            }
        }

        // 성별 처리 (null이면 변경 안함)
        if (newGender != null) {
            String g = newGender.trim().toUpperCase();
            if (g.isEmpty()) {
                throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
            }

            Gender genderEnum;
            try {
                genderEnum = Gender.valueOf(g);
            } catch (IllegalArgumentException ex) {
                throw new ValidationException(ErrorCode.INVALID_INPUT_VALUE);
            }

            if (member.getGender() == null || !member.getGender().equals(genderEnum)) {
                member.updateMemberProfile(null, null, genderEnum);
                changed = true;
            }
        }

        if (changed) {
            memberRepository.save(member);
        }

        return MemberDto.from(member);
    }


}