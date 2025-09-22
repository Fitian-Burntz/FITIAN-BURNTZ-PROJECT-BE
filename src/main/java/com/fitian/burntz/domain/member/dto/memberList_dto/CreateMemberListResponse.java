package com.fitian.burntz.domain.member.dto.memberList_dto;

import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.member.entity.MemberList;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateMemberListResponse {

    private Long memberListPk;
    private Long boxPk;
    private Long ownerPk;
    private MemberRole role;

    public static CreateMemberListResponse toDto(MemberList memberList){
        return CreateMemberListResponse.builder()
                .memberListPk(memberList.getMemberListPk())
                .boxPk(memberList.getBox().getBoxPk())
                .role(memberList.getRole())
                .build();
    }
}
