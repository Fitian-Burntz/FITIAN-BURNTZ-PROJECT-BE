package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.auth.service.RefreshTokenService;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.member.dto.MemberCreateResult;
import com.fitian.burntz.domain.member.dto.MemberDto;
import com.fitian.burntz.domain.member.dto.memberList_dto.BoxAndMemberListDto;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.member_enum.Gender;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.common.util.PreconditionValidator;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.infra.s3.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final MemberListRepository memberListRepository;
    private final RefreshTokenService refreshTokenService;
    private final PreconditionValidator preconditionValidator;
    private final BoxRepository boxRepository;
    private final S3Service s3Service;

    /**
     * 로그인 전용 멤버 정보 반환
     * provider + memberId 조합으로 조회 후 없으면 생성.
     * 동시성 발생 시 DataIntegrityViolationException을 잡아 재조회하여 정상화.
     */
    public MemberCreateResult getOrCreateMember(String provider, String memberId, String nickname, String email) {
        Optional<Member> existing = memberRepository.findByProviderAndMemberIdAndDeletedYN(provider, memberId, BaseTime.Yn.N);

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
            Member savedMember = memberRepository.saveAndFlush(newMember);
            return new MemberCreateResult(savedMember, true);
        } catch (DataIntegrityViolationException dive) {
            // 동시성으로 다른 트랜잭션이 생성했을 가능성 -> 재조회
            Member savedMember = memberRepository.findByProviderAndMemberIdAndDeletedYN(provider, memberId, BaseTime.Yn.N)
                    .orElseThrow(() -> dive); // 예외 재던지기
            return new MemberCreateResult(savedMember, false);
        }
    }

    /** 내 정보 가져오기 (box 관련 내 정보랑은 별개) **/
    public MemberDto getMyInfo(Long memberPk){
        // 기본 파라미터 검증
        memberPk = preconditionValidator.requireMemberPk(memberPk);

        Member member = memberRepository.findActiveById(memberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        return MemberDto.from(member);
    }

    /** member 정보 수정 (default nickname, gender) **/
    public MemberDto updateMemberInfo(Long memberPk, String newNickname, String newGender) {

        // 기본 파라미터 검증
        memberPk = preconditionValidator.requireMemberPk(memberPk);

        Member member = memberRepository.findActiveById(memberPk)
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

    /** 내가 가입한 박스들 정보 가져오기 **/
    public List<BoxAndMemberListDto> getMyBoxes(Long memberPk){
        // 기본 파라미터 검증
        memberPk = preconditionValidator.requireMemberPk(memberPk);

        Member member = memberRepository.findActiveById(memberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        List<MemberList> memberLists = memberListRepository.findAllByMemberMemberPkAndDeletedYN(memberPk, BaseTime.Yn.N);
        List<BoxAndMemberListDto> boxAndMemberListDtoList = new ArrayList<>();

        for(MemberList ml : memberLists) {
            BoxAndMemberListDto dto = BoxAndMemberListDto.builder()
                    .boxPk(ml.getBox().getBoxPk())
                    .boxCode(ml.getBox().getBoxCode())
                    .boxName(ml.getBox().getBoxName())
                    .boxNickname(ml.getBoxNickname())
                    .role(ml.getRole())
                    .build();
            boxAndMemberListDtoList.add(dto);
        }

        return boxAndMemberListDtoList;
    }


    /** 멤버 탈퇴 **/
    public MemberDto withdrawMember(Long memberPk) {

        // 기본 파라미터 검증
        memberPk = preconditionValidator.requireMemberPk(memberPk);

        Member member = memberRepository.findActiveById(memberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        try {
            refreshTokenService.softDeleteAllByMember(memberPk);
        } catch (Exception e) {
            log.error(
                    "failed to soft-delete auth rows for memberPk={}, proceeding with member deletion",
                    memberPk, e
            );
        }

        String oldMemberId = member.getMemberId();

        String ts = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));

        // 예: originalId__DEL__20260205180530123
        String changedMemberId = oldMemberId + "__DEL__" + ts;

        member.changeMemberId(changedMemberId);
        member.markDeleted();

        Member savedMember = memberRepository.save(member);

        // MemberList, Box 탈퇴, Record, ClassParticipant, ChannelParticipant, FcmToken, Membership, MembershipHistory 전부 딜리트 해야함

        return MemberDto.from(savedMember);
    }

    /** 프로필 이미지 업데이트 **/
    public S3Service.ProfileImageUrls updateProfileImage(Long memberPk, Long boxPk, MultipartFile image) {
        memberPk = preconditionValidator.requireMemberPk(memberPk);
        boxPk = preconditionValidator.requireBoxPk(boxPk);

        MemberList memberList = memberListRepository.findActiveByBoxPkAndMemberPk(boxPk, memberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBER_NOT_IN_BOX));

        S3Service.ProfileImageUrls urls = s3Service.uploadProfileImage(memberPk, boxPk, image);
        memberList.updateProfileImageUrl(urls.mediumUrl());
        memberListRepository.save(memberList);

        return urls;
    }

    /** 가장 마지막으로 방문한 Box PK 정보 멤버에 업데이트 **/
    public Long updateLastVisitedBox(Long memberPk, Long boxPk) {

        // 기본 파라미터 검증
        memberPk = preconditionValidator.requireMemberPk(memberPk);
        boxPk = preconditionValidator.requireBoxPk(boxPk);

        // member DB 존재 여부 검증
        Member targetMember = memberRepository.findActiveById(memberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        // box DB 존재 여부 검증
        boxRepository.findActiveById(boxPk)
                        .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        // memberList 에서 member 가 해당 box 에 속해 있는지 검증
        memberListRepository.findActiveByBoxPkAndMemberPk(boxPk, memberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBERLIST_NOT_FOUND));

        targetMember.updateLastVisitedBoxPk(boxPk);

        memberRepository.save(targetMember);

        return boxPk;

    }

}