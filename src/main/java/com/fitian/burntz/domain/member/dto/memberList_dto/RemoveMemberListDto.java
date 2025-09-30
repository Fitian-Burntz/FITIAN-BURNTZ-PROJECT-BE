package com.fitian.burntz.domain.member.dto.memberList_dto;

import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.global.common.entity.BaseTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author : 김남이
 * @packageName : com.fitian.burntz.domain.member.dto.memberList_dto
 * @fileName : RemoveMemberListDto
 * @date : 2025-09-30
 * @description : memberList 삭제 시 데이터 전달용 DTO
 */

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "memberList 삭제 시 데이터 전달 용 DTO")
public class RemoveMemberListDto {
    private Long memberListPk;
    private Long boxPk;
    private Long memberPk;
    private String boxNickname;
    private MemberRole role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private BaseTime.Yn deletedYN;

    public static RemoveMemberListDto entityToDto(MemberList memberList) {
        Objects.requireNonNull(memberList, "memberList required.");

        return RemoveMemberListDto.builder()
                .memberListPk(memberList.getMemberListPk())
                .boxPk(memberList.getBox().getBoxPk())
                .memberPk(memberList.getMember().getMemberPk())
                .boxNickname(memberList.getBoxNickname())
                .role(memberList.getRole())
                .createdAt(memberList.getCreatedAt())
                .updatedAt(memberList.getUpdatedAt())
                .deletedYN(memberList.getDeletedYN())
                .build();
    }

    public static RemoveMemberListDto alreadyDeleted(Long memberListPk, Long boxPk) {

        return RemoveMemberListDto.builder()
                .memberListPk(memberListPk)
                .boxPk(boxPk)
                .deletedYN(BaseTime.Yn.Y)
                .build();
    }

}
