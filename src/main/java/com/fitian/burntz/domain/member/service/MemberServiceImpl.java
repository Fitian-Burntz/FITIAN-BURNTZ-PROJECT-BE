package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.auth.service.RefreshTokenService;
import com.fitian.burntz.domain.member.dto.MemberCreateResult;
import com.fitian.burntz.domain.member.dto.MemberDto;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.member_enum.Gender;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final RefreshTokenService refreshTokenService;

    /**
     * provider + memberId 조합으로 조회 후 없으면 생성. 동시성 발생 시 DataIntegrityViolationException을 잡아 재조회하여 정상화.
     */
    @Override
    public MemberCreateResult getOrCreateMember(String provider, String memberId, String nickname, String email) {
        Optional<Member> existing = memberRepository.findByProviderAndMemberId(provider, memberId);

        if (existing.isPresent()) {
            Member existingMember = existing.get();

            // 탈퇴(soft-delete) 상태면 복구 처리
            if (existingMember.isDeleted()) {
                existingMember.markNotDeleted(); // deletedYn = 'N', updatedAt 초기화만 수행
                Member savedMember = memberRepository.save(existingMember);
                return new MemberCreateResult(savedMember, false);
            }

            return new MemberCreateResult(existingMember, false);
        }

        // 기존 동작을 유지: nickname, email이 null이면 fallback 값 사용
        String safeNickname = (nickname == null || nickname.isBlank()) ? "nickname_unset" : nickname;
        String safeEmail = (email == null || email.isBlank()) ? "email_unset" : email;

        // 정적 팩토리 메서드 사용
        Member newMember = Member.create(memberId, safeNickname, safeEmail, provider);

        try {
            Member savedMember = memberRepository.save(newMember);
            return new MemberCreateResult(savedMember, true);
        } catch (DataIntegrityViolationException dive) {
            // 동시성으로 다른 트랜잭션이 생성했을 가능성 -> 재조회
            Member savedMember = memberRepository.findByProviderAndMemberId(provider, memberId)
                    .orElseThrow(() -> dive); // 예외 재던지기
            return new MemberCreateResult(savedMember, false);
        }
    }

    /**
     * 컨트롤러에서 memberPk를 직접 전달하는 버전 (권장)
     */

    @Override
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
                 changed = member.updateMemberProfile(trimmed, null, null);
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
                changed = member.updateMemberProfile(null, null, genderEnum);
            }
        }

        if (changed) {
            memberRepository.save(member);
        }

        return MemberDto.from(member);
    }


    /** 멤버 탈퇴 **/
    @Override
    public MemberDto withdrawMember(Long memberPk) {
        Member member = memberRepository.findById(memberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        try {
            refreshTokenService.softDeleteAllByMember(memberPk);
        } catch (Exception e) {
            log.error(
                    "failed to soft-delete auth rows for memberPk={}, proceeding with member deletion",
                    memberPk, e
            );
        }

        member.markDeleted();

        Member savedMember = memberRepository.save(member);

        return MemberDto.from(savedMember);
    }

    /** 가장 마지막으로 방문한 Box PK 정보 멤버에 업데이트 **/
    @Override
    public void updateLastVisitedBox(Long memberPk, Long boxPk) {
        memberRepository.updateLastVisitedBoxPk(memberPk, boxPk);
    }

}