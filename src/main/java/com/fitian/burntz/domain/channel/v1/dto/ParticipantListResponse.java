package com.fitian.burntz.domain.channel.v1.dto;

import com.fitian.burntz.domain.box.enums.MemberRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.channel.v1.dto
 * @fileName : ParticipantListResponse
 * @date : 2025-12-18
 * @description : 채널 참여자 목록 리스트 입니다.
 */

@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ParticipantListResponse", description = "채널 참여자 목록 응답")
public class ParticipantListResponse {

    @Schema(description = "멤버 PK", example = "8")
    private Long memberPk;

    @Schema(description = "멤버리스트 PK", example = "12")
    private Long memberListPk;

    @Schema(description = "회원 등급", example = "manager")
    private MemberRole role;

    @Schema(description = "박스 내 닉네임", example = "문정동 이경영")
    private String boxNickname;
}
