package com.fitian.burntz.domain.member.dto.memberList_dto;

import com.fitian.burntz.domain.box.enums.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "memberRole 변경 시 등급 변경 요청 포맷")
public class UpdateMemberRoleRequest {

    @NotNull(message = "boxPk is required")
    private Long boxPk;

    @NotNull(message = "memberPk is required")
    private Long memberPk;

    @NotNull(message = "role is required")
    private MemberRole role;

}
