package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.member.dto.memberList_dto.ChangeOwnerSuccessDto;
import com.fitian.burntz.domain.member.dto.memberList_dto.CreateMemberListResponse;
import com.fitian.burntz.domain.member.dto.memberList_dto.MemberListWithMembershipDto;
import com.fitian.burntz.domain.member.dto.memberList_dto.UpdateMemberRoleDto;
import com.fitian.burntz.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberListService {



    UpdateMemberRoleDto updateMemberRole(Long memberPk,UpdateMemberRoleDto updateMemberRoleDto);
    CreateMemberListResponse createMemberList(Member owner, Long newBoxPk);

    /** boxCode 에 해당하는 box 의 memberList 조회 **/
    Page<MemberListWithMembershipDto> getMemberListsWithMembership(String boxCode, Long operatorPk, Pageable pageable);

    /** box OWNER 양도 **/
    ChangeOwnerSuccessDto changeOwnerForBox(Long operatorPk, Long targetMemberPk, Long boxPk);
}
