package com.fitian.burntz.domain.member.dto.memberList_dto;

import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.member.entity.MemberList;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * @author : 김남이
 * @packageName : com.fitian.burntz.domain.member.dto.memberList_dto
 * @fileName : chageOwnerSuccessDto
 * @date : 2025-09-26
 * @description : box OWNER 양도 성공 시 결과 반환하는 dto
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "OWNER 양도 시 양도 완료 결과 전달 DTO")
public class ChangeOwnerSuccessDto {

    private Long boxPk;
    private Long memberPk;
    private String boxNickname;
    private MemberRole role;
    private LocalDateTime updatedAt;

    public static ChangeOwnerSuccessDto from(MemberList newOwnerMember, Long boxPk, Long targetMemberPk) {
        return ChangeOwnerSuccessDto.builder()
                .boxPk(boxPk)
                .memberPk(targetMemberPk)
                .boxNickname(newOwnerMember.getBoxNickname())
                .role(newOwnerMember.getRole())
                .updatedAt(newOwnerMember.getUpdatedAt())
                .build();
    }
}
