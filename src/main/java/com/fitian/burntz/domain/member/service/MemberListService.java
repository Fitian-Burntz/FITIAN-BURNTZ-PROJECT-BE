package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.member.dto.BoxWithMembershipDto;
import com.fitian.burntz.domain.member.dto.memberList_dto.*;
import com.fitian.burntz.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MemberListService {


    /** box 회원의 등급 변경
     * OWNER, MANAGER 만 변경 가능
     * GUEST, MEMBER, MANGER 로 변경할 수 있음 (양도 x) **/
    UpdateMemberRoleDto updateMemberRole(Long memberPk,UpdateMemberRoleDto updateMemberRoleDto);

    /** box 가 생성될 때 연쇄적으로 box에 해당하는 memberList 생성
     * box 생성자는 자동으로 해당 box 의 OWNER 가 됨. **/
    CreateMemberListResponse createMemberList(Member owner, Long newBoxPk);

    /** boxCode 에 해당하는 box 의 memberList 조회 **/
    Page<MemberListWithMembershipDto> getMemberListsWithMembership(String boxCode, Long operatorPk, Pageable pageable);

    /** box OWNER 양도 **/
    ChangeOwnerSuccessDto changeOwnerForBox(Long operatorPk, Long targetMemberPk, Long boxPk);

    /** 회원 정보 단건 조회 (OWNER, MANAGER 전용) **/
    MemberListWithMembershipDto getMemberWithMembership(Long boxPk, Long operatorPk, Long targetMemberPk);

    /** 내 box 정보 및 멤버십 단건 조회
     * 사용자가 내가 속한 box 정보와 membership 정보를 함께 볼 수 있도록함
     * MemberList 엔티티에서 조회하는게 효율적이어서 메서드를 여기에 두었습니다. **/
    BoxWithMembershipDto getMyBoxWithMembership(Long memberPk, Long boxPk);

    /** 내 box 정보 및 멤버십 전체 조회
     * 사용자가 내가 속한 box 정보와 membership 정보를 함께 볼 수 있도록함
     * MemberList 엔티티에서 조회하는게 효율적이어서 메서드를 여기에 두었습니다. **/
    Page<BoxWithMembershipDto> getMyBoxListWithMembership(Long memberPk, Pageable pageable);

    /** 사용자 내 boxNickname 변경하기 **/
    ChangeMyBoxNicknameDto changeMyBoxNickname(Long memberPk, Long boxPk, String boxNickname);

    /** 해당 memberList soft-delete 처리 **/
    RemoveMemberListDto removeMemberList(Long memberListPk, Long operatorPk, Long boxPk);
}
