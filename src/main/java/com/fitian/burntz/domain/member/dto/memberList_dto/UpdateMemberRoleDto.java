package com.fitian.burntz.domain.member.dto.memberList_dto;

import com.fitian.burntz.domain.box.enums.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "memberRole 변경 시 정보 전달용 DTO")
public class UpdateMemberRoleDto {
    @NotNull(message = "boxPk required")
    private Long boxPk;

    @NotNull(message = "memberPk required")
    private Long memberPk;

    @NotNull(message = "memberPk required")
    private MemberRole role;
    private LocalDateTime updatedAt;

    public static UpdateMemberRoleDto fromRequest(UpdateMemberRoleRequest updateMemberRoleRequest){

        Objects.requireNonNull(updateMemberRoleRequest, "updateMemberRoleRequest required.");

        return UpdateMemberRoleDto.builder()
                .boxPk(updateMemberRoleRequest.getBoxPk())
                .memberPk(updateMemberRoleRequest.getMemberPk())
                .role(updateMemberRoleRequest.getRole())
                .build();
    }

    public static UpdateMemberRoleDto UpdateMemberRoleSuccessDto(
            Long boxPk, Long targetMemberPk, MemberRole updatedNewRole, LocalDateTime updatedAt
    ){
        Objects.requireNonNull(boxPk, "boxPK required.");
        Objects.requireNonNull(targetMemberPk, "targetMemberPk required.");
        Objects.requireNonNull(updatedNewRole, "updatedNewRole required.");
        Objects.requireNonNull(updatedAt, "updatedAt required.");

        return UpdateMemberRoleDto.builder()
                .boxPk(boxPk)
                .memberPk(targetMemberPk)
                .role(updatedNewRole)
                .updatedAt(updatedAt)
                .build();
    }


}
