package com.fitian.burntz.domain.member.dto.memberList_dto;

import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.member.entity.MemberList;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.Objects;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder

@Schema(description = "box 생성 시 연쇄적으로 생성되는 MemberList 정보 생성완료 응답 DTO")
public class CreateMemberListResponse {

    private Long memberListPk;
    private Long boxPk;
    private Long ownerPk;
    private MemberRole role;

    public static CreateMemberListResponse toDto(MemberList memberList){
        Objects.requireNonNull(memberList, "memberList required.");

        return CreateMemberListResponse.builder()
                .memberListPk(memberList.getMemberListPk())
                .boxPk(memberList.getBox().getBoxPk())
                .role(memberList.getRole())
                .build();
    }
}
