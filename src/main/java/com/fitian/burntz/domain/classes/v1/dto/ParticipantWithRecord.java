package com.fitian.burntz.domain.classes.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.classes.v1.dto
 * @fileName : ParticipantWithRecord
 * @date : 2025-12-27
 * @description : 참여자와 참여자의 기록을 반환하는 dto 입니다.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ParticipantWithRecord", description = "수업 참여자 및 기록 응답")
public class ParticipantWithRecord {
    @Schema(description = "참여 PK", example = "10")
    private Long classParticipantPk;

    @Schema(description = "클래스 PK", example = "123")
    private Long classesPk;

    @Schema(description = "멤버 리스트 PK", example = "42")
    private Long memberListPk;

    @Schema(description = "박스 닉네임", example = "문정동 이경영")
    private String boxNickname;

    @Schema(description = "해당 참가자에 대한 record 존재 여부", example = "true")
    private boolean recordExists;

}
