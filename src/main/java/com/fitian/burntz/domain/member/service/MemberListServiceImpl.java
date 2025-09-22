package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.member.dto.memberList_dto.CreateMemberListResponse;
import com.fitian.burntz.domain.member.dto.memberList_dto.UpdateMemberRoleDto;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberListServiceImpl implements MemberListService{

    private final MemberListRepository memberListRepository;
    private final BoxRepository boxRepository;

    /**
     * owner: 로그인한 사용자의 owner (권한 체크 필요 시 사용)
     * updateMemberRoleDto: boxPk, owner(target), role(optional)
     */
    @Override
    public CreateMemberListResponse createMemberList(Member owner, Long boxPk) {
        // 인증 체크: 컨트롤러에서 이미 처리하더라도 방어적으로 검사
        if (owner == null) {
            throw new ValidationException(ErrorCode.UNAUTHORIZED);
        }

        // 이미 해당 박스에 멤버가 존재하는지 확인
        if (memberListRepository.existsActiveByBoxPkAndMemberPk(boxPk, owner.getMemberPk())) {
            throw new ValidationException(ErrorCode.DUPLICATE_MEMBER);
        }

        // 이미 박스 생성 로직에서 요청 멤버의 DB 존재 여부를 확인했음.
        // 박스 조회
        Box box = boxRepository.findById(boxPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.BOX_NOT_FOUND));

        // 정적 팩토리 메서드로 객체 생성
        MemberList memberList = MemberList.create(box, owner);

        try {
            MemberList savedMemberList = memberListRepository.save(memberList);
            log.info("MemberList created: boxPk={} owner={}", boxPk, owner);
            return CreateMemberListResponse.toDto(savedMemberList);
        } catch (DataIntegrityViolationException dive) {
            // 인덱스/동시성 문제 등으로 중복 삽입 시 안전하게 처리
            log.warn("Failed to save MemberList (possible duplicate): boxPk={} owner={}", boxPk, owner, dive);
            throw new ValidationException(ErrorCode.DUPLICATE_MEMBER);
        }
    }

    /** MANAGER, MEMBER 로 역할 변경 가능 (양도 X) **/
    @Override
    public UpdateMemberRoleDto updateMemberRole(Long operatorPk, UpdateMemberRoleDto dto) {
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

        // 요청자가 해당 박스에 속해있는지 확인
        MemberList operator = memberListRepository.findByBox_BoxPkAndMember_MemberPk(boxPk, operatorPk)
                .orElseThrow(() -> new ValidationException(ErrorCode.FORBIDDEN));

        // 요청자는 OWNER 또는 MANAGER 여야 함
        if (!(operator.getRole() == MemberRole.OWNER || operator.getRole() == MemberRole.MANAGER)) {
            throw new ValidationException(ErrorCode.FORBIDDEN);
        }

        // 3) 대상 멤버가 박스에 존재하는지 확인
        MemberList targetMember = memberListRepository.findByBox_BoxPkAndMember_MemberPk(boxPk, targetMemberPk)
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


}
