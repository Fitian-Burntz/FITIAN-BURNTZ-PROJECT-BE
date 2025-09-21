package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.member.dto.memberList_dto.UpdateMemberRoleDto;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.member.repository.MemberListRepository;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberListServiceImpl implements MemberListService{

    private final MemberListRepository memberListRepository;

    @Override
    public UpdateMemberRoleDto updateMemberRole(Long memberPk, UpdateMemberRoleDto dto) {
        // 인증 실패
        if (memberPk == null) {
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
        MemberList operator = memberListRepository.findByBox_BoxPkAndMember_MemberPk(boxPk, memberPk)
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

        // 6) OWNER를 강등할 경우, 박스에 최소한 한 명의 OWNER는 남아야 함
        if (oldRole == MemberRole.OWNER && newRole != MemberRole.OWNER) {
            long ownerCount = memberListRepository.countByBox_BoxPkAndRole(boxPk, MemberRole.OWNER);
            if (ownerCount <= 1) {
                throw new ValidationException(ErrorCode.OPERATION_NOT_ALLOWED);
            }
        }

        // 변경 수행 (도메인 메서드 사용)
        targetMember.changeRole(newRole);
        memberListRepository.save(targetMember); // 명시적 저장 (JPA 변경 감지로도 가능)

        log.info("Changed member role: boxPk={} memberPk={} from={} to={} byOperator={}",
                boxPk, targetMemberPk, oldRole, newRole, memberPk);

        return UpdateMemberRoleDto.builder()
                .boxPk(boxPk)
                .memberPk(targetMemberPk)
                .role(newRole)
                .build();
    }
}
