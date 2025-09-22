package com.fitian.burntz.domain.member.dto.memberList_dto;

import com.fitian.burntz.domain.box.enums.MemberRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateMemberRoleRequest {

    @NotNull(message = "boxPk is required")
    private Long boxPk;

    @NotNull(message = "memberPk is required")
    private Long memberPk;

    @NotNull(message = "role is required")
    private MemberRole role;

}
