package com.fitian.burntz.domain.member.dto.memberList_dto;

import com.fitian.burntz.domain.box.enums.MemberRole;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateMemberRoleDto {
    private Long boxPk;
    private Long memberPk;
    private MemberRole role;
    private LocalDateTime updatedAt;

    public static UpdateMemberRoleDto fromRequest(UpdateMemberRoleRequest updateMemberRoleRequest){
        return UpdateMemberRoleDto.builder()
                .boxPk(updateMemberRoleRequest.getBoxPk())
                .memberPk(updateMemberRoleRequest.getMemberPk())
                .role(updateMemberRoleRequest.getRole())
                .build();
    }

    public static UpdateMemberRoleDto UpdateMemberRoleSuccessDto(
            Long boxPk, Long targetMemberPk, MemberRole updatedNewRole, LocalDateTime updatedAt
    ){
        return UpdateMemberRoleDto.builder()
                .boxPk(boxPk)
                .memberPk(targetMemberPk)
                .role(updatedNewRole)
                .updatedAt(updatedAt)
                .build();
    }


}
