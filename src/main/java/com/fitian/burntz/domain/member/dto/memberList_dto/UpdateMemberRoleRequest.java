package com.fitian.burntz.domain.member.dto.memberList_dto;

import com.fitian.burntz.domain.box.enums.MemberRole;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateMemberRoleRequest {

    private Long boxPk;
    private Long memberPk;
    private MemberRole role;

}
