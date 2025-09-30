package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.member.dto.BoxWithMembershipDto;
import com.fitian.burntz.domain.member.dto.memberList_dto.*;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.domain.membership.entity.Membership;
import com.fitian.burntz.domain.membership.repository.MembershipRepository;
import com.fitian.burntz.domain.membership.v1.dto.MembershipDto;
import com.fitian.burntz.global.common.util.PreconditionValidator;
import com.fitian.burntz.global.common.util.RetryTransactionalHandler;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import jakarta.persistence.EntityManager;
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

import static com.fitian.burntz.global.common.entity.BaseTime.Yn.N;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberListServiceImpl implements MemberListService{

    private final MemberListRepository memberListRepository;
    private final BoxRepository boxRepository;
    private final MemberRepository memberRepository;
    private final MembershipRepository membershipRepository;
    private final RetryTransactionalHandler retryTransactionalHandler;
    private final EntityManager em;
    private final PreconditionValidator preconditionValidator;
    private final MemberService memberService;

    /** box 와 연관된 memberList 생성. box 생성 및 member 추가 시 종속적으로 생성 **/
    @Override
    @Transactional
    public CreateMemberListResponse createMemberList(Member owner, Long newBoxPk) {
        // 인증 체크: 컨트롤러에서 이미 처리하더라도 방어적으로 검사
        if (owner == null) {
            throw new ValidationException(ErrorCode.UNAUTHORIZED);
        }

        Long ownerPk = owner.getMemberPk();

        // 이미 해당 박스에 멤버가 존재하는지 확인
        if (memberListRepository.existsActiveByBoxPkAndMemberPk(newBoxPk, ownerPk)) {
            throw new ValidationException(ErrorCode.DUPLICATE_MEMBER);
        }

        // 이미 박스 생성 로직에서 요청 멤버의 DB 존재 여부를 확인했음.
        // 활성화 box DB 존재 여부 검증 및 엔티티 조회
        Box newBox = boxRepository.findActiveById(newBoxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));


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

    /** MANAGER, MEMBER, GUEST 로 역할 변경 가능 (양도 X) **/
    @Override
    @Transactional
    public UpdateMemberRoleDto updateMemberRole(Long operatorPk, UpdateMemberRoleDto updateMemberRoleDto) {
        //서비스에서 한 번 더 필요 데이터 체크 (방어적 코딩)
        // 기본 파라미터 검증
        operatorPk = preconditionValidator.requireMemberPk(operatorPk);

        // 필요 데이터 불충분
        if (updateMemberRoleDto == null ||
                updateMemberRoleDto.getBoxPk() == null ||
                updateMemberRoleDto.getMemberPk() == null ||
                updateMemberRoleDto.getRole() == null)
        {
            throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        Long boxPk = updateMemberRoleDto.getBoxPk();
        Long targetMemberPk = updateMemberRoleDto.getMemberPk();
        MemberRole newRole = updateMemberRoleDto.getRole();

        // 요청 멤버, 변경 멤버 활성 상태 DB 존재 여부 검증
        memberRepository.findActiveById(operatorPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        memberRepository.findActiveById(targetMemberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        // 박스 활성 상태 검증
        boxRepository.findActiveById(boxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        // 요청자가 해당 박스에 속해있는지 확인
        MemberList operator = memberListRepository.findActiveByBoxPkAndMemberPk(boxPk, operatorPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.FORBIDDEN));


        // 대상 멤버가 박스에 존재하는지 확인
        MemberList targetMember = memberListRepository.findActiveByBoxPkAndMemberPk(boxPk, targetMemberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        // updateMemberRole 메서드 관련 필요 검증 헬퍼 호출
        MemberRole oldRole = updateMemberRoleValidate(operator, targetMember, newRole);


        // 변경 수행 (도메인 메서드 사용)
        targetMember.changeRole(newRole);
        memberListRepository.save(targetMember); // 명시적 저장 (JPA 변경 감지로도 가능)

        log.info("Changed member role: boxPk={} operatorPk={} from={} to={} byOperator={}",
                boxPk, targetMemberPk, oldRole, newRole, operatorPk);

        return UpdateMemberRoleDto.UpdateMemberRoleSuccessDto(
                boxPk, targetMemberPk, newRole, targetMember.getUpdatedAt()
        );

    }

    /** 내 box 정보와 멤버십 통합 정보 단건 조회
     * 조회용 메서드지만 연쇄 작용으로 마지막 방문 boxPk 를 업데이트하므로
     * Transactional 이 필요합니다. **/
    @Override
    @Transactional
    public BoxWithMembershipDto getMyBoxWithMembership(Long memberPk, Long boxPk) {
        // 기본 파라미터 검증
        memberPk = preconditionValidator.requireMemberPk(memberPk);
        boxPk = preconditionValidator.requireBoxPk(boxPk);

        // member DB 존재 여부 확인
        memberRepository.findActiveById(memberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.UNAUTHORIZED));

        // box DB 존재 여부 확인
        Box targetBox = boxRepository.findActiveById(boxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        // memberList (회원이 해당 box에 속해있는지 검사 및 box fetch)
        MemberList loginMemberList = memberListRepository.findActiveByBoxPkAndMemberPk(boxPk, memberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBERLIST_NOT_FOUND)); // 또는 BOX_NOT_FOUND

        // 최신 멤버십 조회 (없을 수 있음 -> optional)
        MembershipDto targetMembershipDto = membershipRepository
                .findLatestByBoxPkAndMemberPk(boxPk, memberPk)
                .map(MembershipDto::from)
                .orElse(null);


        // 마지막 방문 boxPk 기록 업데이트
        memberService.updateLastVisitedBox(memberPk, boxPk);


        // DTO 조립 (memberList 포함)
        return BoxWithMembershipDto.from(loginMemberList, targetBox, targetMembershipDto);
    }

    /** 내 box 정보 및 멤버십 정보 전체 조회 **/
    @Override
    @Transactional(readOnly = true)
    public Page<BoxWithMembershipDto> getMyBoxListWithMembership(Long memberPk, Pageable pageable) {

        // 기본 파라미터 검증
        preconditionValidator.requireMemberPk(memberPk);

        memberRepository.findActiveById(memberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.UNAUTHORIZED));

        Page<MemberList> memberListPage = memberListRepository.findActiveByMemberPkWithBox(memberPk, pageable);

        if (memberListPage.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, 0);
        }

        // boxPk 목록
        List<Long> boxPkList = memberListPage.stream()
                .map(memberList -> memberList.getBox().getBoxPk())
                .collect(Collectors.toList());

        // 박스들에 대해 해당 멤버의 최신 멤버십들을 한 번에 조회
        List<Membership> latestMemberships = membershipRepository.findLatestMembershipsForMemberByBoxes(boxPkList, memberPk);

        // boxPk -> membership 매핑
        Map<Long, Membership> membershipByBoxPk = latestMemberships.stream()
                .collect(Collectors.toMap(membership -> membership.getBox().getBoxPk(),
                        membership -> membership));

        // DTO 매핑 (memberList 정보 포함)
        List<BoxWithMembershipDto> boxWithMembershipDtoList = memberListPage.stream().map(memberList -> {
            Box targetBox = memberList.getBox();
            Membership targetMembership = membershipByBoxPk.get(targetBox.getBoxPk());
            MembershipDto targetMembershipDto = targetMembership == null ? null : MembershipDto.from(targetMembership);
            return BoxWithMembershipDto.from(memberList, targetBox, targetMembershipDto);
        }).collect(Collectors.toList());

        return new PageImpl<>(boxWithMembershipDtoList, pageable, memberListPage.getTotalElements());
    }

    /** 내 box nickname 바꾸기 **/
    @Override
    @Transactional
    public ChangeMyBoxNicknameDto changeMyBoxNickname(Long memberPk, Long boxPk, String boxNickname) {

        // 기본 검증
        Long loginMemberPk = preconditionValidator.requireMemberPk(memberPk);
        Long targetBoxPk = preconditionValidator.requireBoxPk(boxPk);
        String newBoxNickname = preconditionValidator.requiredStringValue(boxNickname);

        // 멤버 DB 존재 여부 검증
        memberRepository.findActiveById(loginMemberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        // box DB 존재 여부 검증
        boxRepository.findActiveById(targetBoxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        // member 가 해당 box 에 속해있는지 검증
        MemberList loginMember = memberListRepository.findActiveByBoxPkAndMemberPk(boxPk, loginMemberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBERLIST_NOT_FOUND));


        loginMember.changeMyBoxNickname(newBoxNickname);
        memberListRepository.save(loginMember);

        return ChangeMyBoxNicknameDto.from(loginMember, targetBoxPk);
    }

    /** 회원 정보 단건 조회 (OWNER, MANAGER 전용) **/
    @Override
    @Transactional(readOnly = true)
    public MemberListWithMembershipDto getMemberWithMembership(Long boxPk, Long operatorPk, Long targetMemberPk) {
        // 기본 파라미터 검증
        operatorPk = preconditionValidator.requireMemberPk(operatorPk);
        targetMemberPk = preconditionValidator.requireMemberPk(targetMemberPk);
        boxPk = preconditionValidator.requireBoxPk(boxPk);


        // 활성화 member DB 존재 여부 확인
        memberRepository.findActiveById(operatorPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));
        memberRepository.findActiveById(targetMemberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));


        // 활성화 box DB 존재 여부 확인
        Box targetBox = boxRepository.findActiveById(boxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));


        // 요청자가 OWNER 권한이면 통과, 아니면  memberList 정보로 권한 검증
        if (!Objects.equals(targetBox.getOwnerPk(), operatorPk)) {
            // memberList 에서 해당 정보 요청자(operator)가해당 box 에 속해있는지 검증
            MemberList operator = memberListRepository.findActiveByBoxPkAndMemberPk(boxPk, operatorPk)
                    .orElseThrow(() -> new ValidationException(ErrorCode.MEMBERLIST_NOT_FOUND));


            // 요청자가 MANAGER 권한 이상이 아닌 경우 권한 오류
            if (operator.getRole() == MemberRole.MEMBER || operator.getRole() == MemberRole.GUEST) {
                throw new ValidationException(ErrorCode.FORBIDDEN);
            }
        }

        // memberList 에서 해당 회원이 해당 box 에 속해있는지 검증 및 조회
        MemberList targetMember = memberListRepository.findActiveByBoxPkAndMemberPk(boxPk, targetMemberPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBERLIST_NOT_FOUND));

        MembershipDto targetMembership = membershipRepository
                .findLatestByBoxPkAndMemberPk(boxPk, targetMemberPk)
                .map(MembershipDto::from)
                .orElse(null);


        return MemberListWithMembershipDto.from(targetMember, targetMemberPk, boxPk, targetMembership);

    }



    /** box memberList 정보 가져오기(with membership) **/
    @Override
    @Transactional(readOnly = true)
    public Page<MemberListWithMembershipDto> getMemberListsWithMembership(
            String boxCode, Long operatorPk, Pageable pageable
    ) {

        // 기본 파라미터 검증
        boxCode = preconditionValidator.requireBoxCode(boxCode);
        operatorPk = preconditionValidator.requireMemberPk(operatorPk);

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
                : membershipRepository.findLatestMembershipPerMemberByBox(boxPk, memberPks);

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

    /** OWNER 양도 서비스 로직
     * lock 거는 row를 최소화 하기 위해서
     * 코드 순서 매우 중요 **/
    @Override
    public ChangeOwnerSuccessDto changeOwnerForBox(Long operatorPk, Long targetMemberPk, Long boxPk){
        return retryTransactionalHandler.executeWithRetry(() -> {

            // 기본 파라미터 검증
            if (operatorPk == null) {
                throw new ValidationException(ErrorCode.UNAUTHORIZED);
            }

            if (targetMemberPk == null) {
                throw new ValidationException(ErrorCode.UNAUTHORIZED);
            }

            if (boxPk == null) {
                throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
            }

            // 활성화 targetMemberPk DB 존재 여부 검증
            memberRepository.findActiveById(operatorPk)
                    .orElseThrow(() -> new ValidationException(ErrorCode.UNAUTHORIZED));
            memberRepository.findActiveById(targetMemberPk)
                    .orElseThrow(() -> new ValidationException(ErrorCode.UNAUTHORIZED));

            // 엔티티 꺼내기 및 활성화 box 검증 (해당 row 에 lock)
            Box targetBox = boxRepository.findActiveBoxByIdWithLock(boxPk)
                    .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

            // box owner 권한 검증
            if (!Objects.equals(targetBox.getOwnerPk(), operatorPk)) {
                throw new ValidationException(ErrorCode.FORBIDDEN);
            }

            // lock 걸고 활성화 memberList 에서 해당 box 에 OWNER 가 한 명인지 검증
            // OWNER 는 반드시 한 명만 존재
            // 상위 결과 2개만 lock
            List<MemberList> ownerList = memberListRepository.findTop2ByBox_BoxPkAndRoleAndDeletedYN(boxPk, MemberRole.OWNER, N);
            if (ownerList.size() != 1) throw new ValidationException(ErrorCode.OWNER_INVALID_STATE);

            // memberList 에서 OWNER 정보 가져오기
            MemberList currentOwner = ownerList.get(0);

            // box.ownerPk 와 memberList 상의 owner 일치 여부 확인
            if (!Objects.equals(currentOwner.getMember().getMemberPk(), targetBox.getOwnerPk())) {
                // 데이터 정합성 깨짐: 운영 이슈로 처리
                throw new ValidationException(ErrorCode.OWNER_INVALID_STATE);
            }


            // memberList 상의 OWNER 의 memberPk 값 가져오기
            Long currentOwnerPk = currentOwner.getMember().getMemberPk();

            // operatorPk 가 활성화 해당 박스에 속해있는지 확인 및  memberList 상의 정보와 일치하는지 확인
            MemberList operatorMember;
            if (Objects.equals(currentOwnerPk, operatorPk)) {
                operatorMember = currentOwner; // 이미 락 걸려 있음 -> 재사용
            } else {
                // 이 분기는 정상 상태라면 발생하지 않음. 발생하면 데이터 정합성 문제.
                log.error("Operator does not match current owner: box.ownerPk={}, currentOwner.memberPk={}, operatorPk={}",
                        targetBox.getOwnerPk(), currentOwner.getMember().getMemberPk(), operatorPk);
                throw new ValidationException(ErrorCode.OWNER_INVALID_STATE);
            }

            // 양도 대상자가 이미 OWNER 이면 에러
            if (Objects.equals(currentOwnerPk, targetMemberPk)) {
                throw new ValidationException(ErrorCode.NO_CHANGE_REQUIRED);
            }

            // 양도 대상자가 해당 박스에 속해있는지 확인
            MemberList targetMember = memberListRepository.findActiveMemberListByBoxAndMemberWithLock(boxPk, targetMemberPk)
                    .orElseThrow(() -> new ValidationException(ErrorCode.MEMBERLIST_NOT_FOUND));

            // 양도 대상자가 box의 매니저인지 확인(매니저에게만 양도 가능)
            if (!Objects.equals(targetMember.getRole(), MemberRole.MANAGER)){
                throw new ValidationException(ErrorCode.ONLY_MANAGER_CAN_BE_OWNER);
            }

            // 변경 순서: 기존 Owner 먼저 강등 -> 대상자 승격 -> box.ownerPk 동기화
            operatorMember.changeRole(MemberRole.MEMBER); // 기존 owner 강등
            targetMember.changeRole(MemberRole.OWNER);   // 대상자 승격
            targetBox.changeBoxOwnerPk(targetMemberPk);  // box.ownerPk 동기화

            em.flush(); // JPA 더티 체킹으로 자동으로 변경 사항이 반영되지만 명시적으로 DB 반영

             // targetBox 에 대한 변경 사항 반영 다시 덮어쓰기
            // (이미 lock 을 걸어서 targetBox 를 조회 했으므로 여전히 해당 row lock 상태에서 refresh)
            em.refresh(targetBox);

            //== 재검증 로직 ==/

            // lock 걸고 OWNER 양도 후 활성화 memberList 에서 해당 box 에 OWNER 가 한 명인지 재검증 (OWNER 는 반드시 한 명만 존재)
            List<MemberList> newOwnerList = memberListRepository.findTop2ByBox_BoxPkAndRoleAndDeletedYN(boxPk, MemberRole.OWNER, N);
            if (newOwnerList.size() != 1) throw new ValidationException(ErrorCode.OWNER_INVALID_STATE);

            MemberList newOwnerMember = newOwnerList.get(0);
            Long  newOwnerPk = newOwnerMember.getMember().getMemberPk();

            // memberList 에 OWNER 의 memberPk 가 targetMemberPk 로 잘 변경됐는지 검증
            if (!Objects.equals(newOwnerPk, targetMemberPk)) {
                throw new ValidationException(ErrorCode.CHANGE_OWNER_FAIL);
            }

            // box 에서도 ownerPk 가 newOwnerPk 로 잘 동기화 변경 됐는지 확인
            if (!Objects.equals(targetBox.getOwnerPk(), newOwnerPk)) {
                throw new ValidationException(ErrorCode.CHANGE_OWNER_FAIL);
            }


            return ChangeOwnerSuccessDto.from(newOwnerMember, boxPk, targetMemberPk);
        });
    }

    /** memberList soft-delete 처리 **/
    @Override
    @Transactional
    public RemoveMemberListDto removeMemberList(Long memberListPk, Long operatorPk, Long boxPk) {
        // 기본 파라미터 검증
        memberListPk = preconditionValidator.requireLongValue(memberListPk);
        operatorPk = preconditionValidator.requireMemberPk(operatorPk);
        boxPk = preconditionValidator.requireBoxPk(boxPk);

        // 요청자 DB 존재 여부 검증
        memberRepository.findActiveById(operatorPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.USER_NOT_FOUND));

        // 해당 box DB 존재 여부 검증
        Box targetBox = boxRepository.findActiveById(boxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        // memberList 삭제 요청자가 해당 box 의 OWNER 인지 검증
        if(!Objects.equals(targetBox.getOwnerPk(), operatorPk)) {
            throw new ValidationException(ErrorCode.FORBIDDEN);
        }

        // 요청자가 해당 box 에 속해 있는지 검증 및 해당 memberList 활성화 여부 검증
        memberListRepository.findActiveByBoxPkAndMemberPk(boxPk, operatorPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.MEMBERLIST_NOT_FOUND));

        // memberListPk의 간단한 DB 존재 여부 체크
        Optional<MemberList> optionalMemberList = memberListRepository.findActiveById(memberListPk);
        if (optionalMemberList.isEmpty()) {
            // 멱등 처리: 이미 삭제되었거나 없음 -> 그냥 리턴(또는 로그)
            log.info("memberList already deleted or not found. memberListPk={}", memberListPk);

            return RemoveMemberListDto.alreadyDeleted(memberListPk, boxPk);
        }

        // memberList DB 존재 시 optional 에서 엔티티 가져오기
        MemberList targetMemberList = optionalMemberList.get();

        // 삭제 하려는 memberList 의 boxPk 가 OWNER 의 소속 boxPk 와 일치하는지 검증
        Long targetMemberList_boxPk = targetMemberList.getBox().getBoxPk();
        if (!Objects.equals(boxPk, targetMemberList_boxPk)) {
            throw new ValidationException(ErrorCode.BOX_MISMATCH);
        }

        // 삭제 상태로 변경
        targetMemberList.markDeleted();

        // 변경 사항 명시적으로 저장
        memberListRepository.save(targetMemberList);

        return RemoveMemberListDto.entityToDto(targetMemberList);
    }


    /** 헬퍼 메서드 **/

    // box memberList 에서 특정 멤버의 role 변경 수행 시(MEMBER, MANAGER) 검증 수행 (양도 x)
    private MemberRole updateMemberRoleValidate(MemberList operator, MemberList targetMember, MemberRole newRole) {
        MemberRole oldRole = targetMember.getRole();

        // updateMemberRole 메서드로 OWNER 양도를 시도하는 경우 에러
        if (newRole == MemberRole.OWNER){
            throw new ValidationException(ErrorCode.PROMOTE_TO_OWNER_NOT_ALLOWED);
        }

        // 요청자는 OWNER 또는 MANAGER 여야 함
        if (!(operator.getRole() == MemberRole.OWNER || operator.getRole() == MemberRole.MANAGER)) {
            throw new ValidationException(ErrorCode.FORBIDDEN);
        }

        // 같은 역할로 변경하려는 경우
        if (oldRole == newRole) {
            throw new ValidationException(ErrorCode.NO_CHANGE_REQUIRED);
        }

        // 권한 계층 관련 검증
        // MANAGER는 OWNER의 역할을 변경할 수 없다
        if (operator.getRole() == MemberRole.MANAGER && oldRole == MemberRole.OWNER) {
            throw new ValidationException(ErrorCode.FORBIDDEN);
        }

        return oldRole;
    }

}
