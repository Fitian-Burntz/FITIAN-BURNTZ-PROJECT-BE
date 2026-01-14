package com.fitian.burntz.domain.member.dto.memberList_dto;

import com.fitian.burntz.domain.box.enums.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.member.dto.memberList_dto
 * @fileName : BoxAndMemberListDto
 * @date : 2026-01-14
 * @description : 박스 정보및 해당 유저의 MemberList 정보를 결합한 dto 입니다.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoxAndMemberListDto {

    @Schema(description = "박스 Pk", example = "1")
    private Long boxPk;

    @Schema(description = "박스 코드", example = "burntz")
    private String boxCode;

    @Schema(description = "박스 이름", example = "Crossfit Burntz")
    private String boxName;

    @Schema(description = "멤버 등급", example = "MEMBER")
    private MemberRole role;

    @Schema(description = "박스 닉네임", example = "문정 이경영")
    private String boxNickname;
}
