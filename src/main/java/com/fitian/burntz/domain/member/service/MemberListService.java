package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.member.dto.memberList_dto.CreateMemberListResponse;
import com.fitian.burntz.domain.member.dto.memberList_dto.UpdateMemberRoleDto;
import com.fitian.burntz.domain.member.entity.Member;

public interface MemberListService {



    UpdateMemberRoleDto updateMemberRole(Long memberPk,UpdateMemberRoleDto updateMemberRoleDto);
    CreateMemberListResponse createMemberList(Member owner, Long boxPk);
}
