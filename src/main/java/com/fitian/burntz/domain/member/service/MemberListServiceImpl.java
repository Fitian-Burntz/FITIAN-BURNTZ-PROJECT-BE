package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.member.dto.memberList_dto.CreateMemberListResponse;
import com.fitian.burntz.domain.member.dto.memberList_dto.MemberListWithMembershipDto;
import com.fitian.burntz.domain.member.dto.memberList_dto.UpdateMemberRoleDto;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.domain.membership.entity.Membership;
import com.fitian.burntz.domain.membership.repository.MembershipRepository;
import com.fitian.burntz.domain.membership.v1.dto.MembershipDto;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberListServiceImpl implements MemberListService{

    private final MemberListRepository memberListRepository;
    private final BoxRepository boxRepository;
    private final MemberRepository memberRepository;
    private final MembershipRepository membershipRepository;

    /**
     * owner: 로그인한 사용자의 owner (권한 체크 필요 시 사용)
     * updateMemberRoleDto: newBox, owner(target), role(optional)
     */
    @Override
    public CreateMemberListResponse createMemberList(Member owner, Box newBox) {
        // 인증 체크: 컨트롤러에서 이미 처리하더라도 방어적으로 검사
        if (owner == null) {
            throw new ValidationException(ErrorCode.UNAUTHORIZED);
        }

        Long ownerPk = owner.getMemberPk();
        Long newBoxPk = newBox.getBoxPk();

        // 이미 해당 박스에 멤버가 존재하는지 확인
        if (memberListRepository.existsActiveByBoxPkAndMemberPk(newBoxPk, ownerPk)) {
            throw new ValidationException(ErrorCode.DUPLICATE_MEMBER);
        }

        // 이미 박스 생성 로직에서 요청 멤버의 DB 존재 여부를 확인했음.
        // 박스 조회
        Box box = boxRepository.findActiveById(newBoxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        //생성 후 저장한 박스 엔티티를 바로 받아오는거라서 재조회 필요 없음
        // 정적 팩토리 메서드로 객체 생성
        MemberList memberList = MemberList.create(newBox, owner);

        try {
            MemberList savedMemberList = memberListRepository.save(memberList);
            log.info("MemberList created: newBoxPk={} ownerPk={}", newBoxPk, ownerPk);
            return CreateMemberListResponse.toDto(savedMemberList);
        } catch (DataIntegrityViolationException dive) {
            // 인덱스/동시성 문제 등으로 중복 삽입 시 안전하게 처리
            log.warn("Failed to save MemberList (possible duplicate): newBoxPk={} ownerPk={}", newBoxPk, ownerPk, dive);
            throw new ValidationException(ErrorCode.DUPLICATE_MEMBER);
        }
    }

    /** MANAGER, MEMBER 로 역할 변경 가능 (양도 X) **/
    @Override
    public UpdateMemberRoleDto updateMemberRole(Long operatorPk, UpdateMemberRoleDto dto) {
        //서비스에서 한 번 더 필요 데이터 체크 (방어적 코딩)
        // 인증 실패
        if (operatorPk == null) {
            throw new ValidationException(ErrorCode.UNAUTHORIZED);
        }

        // 필요 데이터 불충분
        if (dto == null || dto.getBoxPk() == null || dto.getMemberPk() == null || dto.getRole() == null) {
            throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        Long boxPk = dto.getBoxPk();
        Long targetMemberPk = dto.getMemberPk();
        MemberRole newRole = dto.getRole();

        // 요청 멤버, 변경 멤버 활성 상태 DB 존재 여부 검증
        memberRepository.findActiveById(operatorPk);
        memberRepository.findActiveById(targetMemberPk);

        // 박스 활성 상태 검증
        boxRepository.findActiveById(boxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        // 요청자가 해당 박스에 속해있는지 확인
        MemberList operator = memberListRepository.findActiveByBoxPkAndMemberPk(boxPk, operatorPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.FORBIDDEN));

        // 요청자는 OWNER 또는 MANAGER 여야 함
        if (!(operator.getRole() == MemberRole.OWNER || operator.getRole() == MemberRole.MANAGER)) {
            throw new ValidationException(ErrorCode.FORBIDDEN);
        }

        // 이 메서드로 OWNER 양도를 시도하는 경우
        if (newRole == MemberRole.OWNER){
            throw new ValidationException(ErrorCode.PROMOTE_TO_OWNER_NOT_ALLOWED);
        }

        // 대상 멤버가 박스에 존재하는지 확인
        MemberList targetMember = memberListRepository.findActiveByBoxPkAndMemberPk(boxPk, targetMemberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        MemberRole oldRole = targetMember.getRole();

        // 같은 역할로 변경하려는 경우
        if (oldRole == newRole) {
            throw new ValidationException(ErrorCode.NO_CHANGE_REQUIRED);
        }

        // 권한 계층 관련 검증
        // MANAGER는 OWNER의 역할을 변경할 수 없다
        if (operator.getRole() == MemberRole.MANAGER && oldRole == MemberRole.OWNER) {
            throw new ValidationException(ErrorCode.FORBIDDEN);
        }

//        // 6) OWNER를 강등할 경우, 박스에 최소한 한 명의 OWNER는 남아야 함
//        if (oldRole == MemberRole.OWNER && newRole != MemberRole.OWNER) {
//            long ownerCount = memberListRepository.countByBox_BoxPkAndRole(boxPk, MemberRole.OWNER);
//            if (ownerCount <= 1) {
//                throw new ValidationException(ErrorCode.OPERATION_NOT_ALLOWED);
//            }
//        }

        // 변경 수행 (도메인 메서드 사용)
        targetMember.changeRole(newRole);
        memberListRepository.save(targetMember); // 명시적 저장 (JPA 변경 감지로도 가능)

        log.info("Changed member role: boxPk={} operatorPk={} from={} to={} byOperator={}",
                boxPk, targetMemberPk, oldRole, newRole, operatorPk);

        return UpdateMemberRoleDto.builder()
                .boxPk(boxPk)
                .memberPk(targetMemberPk)
                .role(newRole)
                .build();
    }


    @Transactional(readOnly = true)
    public Page<MemberListWithMembershipDto> getMemberListsWithMembership(
            String boxCode, Long operatorPk, Pageable pageable
    ) {

        // 기본 검증
        if (boxCode == null) {
            throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
        }
        if (operatorPk == null) {
            throw new ValidationException(ErrorCode.UNAUTHORIZED);
        }

        // Box 조회
        Box box = boxRepository.findByBoxCode(boxCode.trim())
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        Long boxPk = box.getBoxPk();

        // 권한 체크: 오너이거나 OWNER/MANAGER 여야 함
        if (!Objects.equals(box.getOwnerPk(), operatorPk)) {
            MemberList requesterMl = memberListRepository.findActiveByBoxPkAndMemberPk(boxPk, operatorPk)
                    .orElseThrow(() -> new ValidationException(ErrorCode.FORBIDDEN));

            MemberRole role = requesterMl.getRole();
            if (!(role == MemberRole.OWNER || role == MemberRole.MANAGER)) {
                throw new ValidationException(ErrorCode.FORBIDDEN);
            }
        }

        // 1) MemberList 페이지 조회 (JOIN FETCH member, JPQL에서 ORDER BY m.nickname ASC)
        Page<MemberList> memberListPage = memberListRepository.findActiveByBoxPkWithMember(boxPk, pageable);
        List<MemberList> memberLists = memberListPage.getContent();
        if (memberLists.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, memberListPage.getTotalElements());
        }

        // 2) 페이지 내 memberPk 목록 추출 (중복 제거)
        List<Long> memberPks = memberLists.stream()
                .map(memberList -> memberList.getMember().getMemberPk())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        // 3) 멤버별 최신 membership(최대 membership_pk) 한 건씩 조회 — 페이지 단위이므로 단일 호출
        List<Membership> memberships = memberPks.isEmpty()
                ? Collections.emptyList()
                : membershipRepository.findLatestByMaxPkPerMemberNative(boxPk, memberPks);

        // 4) memberPk -> Membership (최신 1건) 매핑
        Map<Long, Membership> latestByMemberPk = memberships.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        membership -> membership.getMember().getMemberPk(),
                        Function.identity()
                ));

        // 5) DTO 조립 — 단일 MembershipDto 전달
        List<MemberListWithMembershipDto> dtos = memberLists.stream().map(memberList -> {
            Long memberPk = memberList.getMember().getMemberPk();
            Membership latest = latestByMemberPk.get(memberPk);

            // 여기서 단일 MembershipDto 생성 (없으면 null)
            MembershipDto membershipDto = (latest == null) ? null : MembershipDto.from(latest);

            // from 메서드는 MembershipDto (단일) 파라미터를 받도록 DTO에 정의되어 있어야 함
            return MemberListWithMembershipDto.from(memberList, memberPk, boxPk, membershipDto);
        }).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, memberListPage.getTotalElements());
    }

}
