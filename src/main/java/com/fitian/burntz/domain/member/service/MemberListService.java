package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.member.dto.BoxWithMembershipDto;
import com.fitian.burntz.domain.member.dto.memberList_dto.*;
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

    /** 회원 정보 단건 조회 (OWNER, MANAGER 전용) **/
    MemberListWithMembershipDto getMemberWithMembership(Long boxPk, Long operatorPk, Long targetMemberPk);

    /** 사용자가 내가 속한 box 정보와 membership 정보를 함께 볼 수 있도록함
     * MemberList 엔티티에서 조회하는게 효율적이어서 메서드를 여기에 두었습니다. **/
    Page<BoxWithMembershipDto> getMyBoxesWithMembership(Long memberPk, Pageable pageable);

    /** 사용자 내 boxNickname 변경하기 **/
    ChangeMyBoxNicknameDto changeMyBoxNickname(Long memberPk, Long boxPk, String boxNickname);
}
