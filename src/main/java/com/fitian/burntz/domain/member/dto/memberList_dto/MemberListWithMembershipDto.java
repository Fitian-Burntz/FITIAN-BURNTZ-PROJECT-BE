package com.fitian.burntz.domain.member.dto.memberList_dto;

import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.membership.v1.dto.MembershipDto;
import lombok.*;

import java.util.List;
import java.util.Objects;

/**
 * @author         : 김남이
 * @packageName    : com.fitian.burntz.domain.member.dto.memberList_dto
 * @fileName : MemberListWithMembershipDto
 * @date : 2025-09-25
 * @description : box 에 해당하는 memberList 를 membership 과 함께 보여주기 위한 Dto 입니다.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberListWithMembershipDto {

    private Long memberListPk;
    private Long memberPk;
    private Long boxPk;
    private String boxNickname;
    private MemberRole role;
    private MembershipDto membership;

    public static MemberListWithMembershipDto from(MemberList memberList, Long memberPk, Long boxPk, MembershipDto membershipDto) {

        Objects.requireNonNull(memberList, "memberList required");
        Objects.requireNonNull(memberPk, "memberPk required");
        Objects.requireNonNull(boxPk, "boxPk required");

        return MemberListWithMembershipDto.builder()
                .memberListPk(memberList.getMemberListPk())
                .memberPk(memberPk)
                .boxPk(boxPk)
                .boxNickname(memberList.getBoxNickname())
                .role(memberList.getRole())
                .membership(membershipDto)
                .build();
    }


}
