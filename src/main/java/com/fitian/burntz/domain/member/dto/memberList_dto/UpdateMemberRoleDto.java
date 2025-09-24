package com.fitian.burntz.domain.member.dto.memberList_dto;

import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.member.entity.MemberList;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateMemberRoleDto {
    private Long boxPk;
    private Long memberPk;
    private MemberRole role;

    public static UpdateMemberRoleDto from(UpdateMemberRoleRequest updateMemberRoleRequest){
        return UpdateMemberRoleDto.builder()
                .boxPk(updateMemberRoleRequest.getBoxPk())
                .memberPk(updateMemberRoleRequest.getMemberPk())
                .role(updateMemberRoleRequest.getRole())
                .build();
    }
}
