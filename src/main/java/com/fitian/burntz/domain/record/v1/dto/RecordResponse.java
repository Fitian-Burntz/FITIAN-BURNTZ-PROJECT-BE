package com.fitian.burntz.domain.record.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.record.v1.dto
 * @fileName : RecordResponse
 * @date : 2025-09-17
 * @description : Record 조회 DTO
 */

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecordResponse {
    @Schema(description = "member Pk")
    private Long memberPk;
    
    @Schema(description = "record Pk")
    private Long RecordPk;

    @Schema(description = "wod Pk")
    private Long WodPk;
    
    @Schema(description = "nickname")
    private String nickname;
    
    @Schema(description = "단계")
    private String level;
    
    @Schema(description = "라운드")
    private Integer round;
    
    @Schema(description = "렙스")
    private Integer reps;
    
    @Schema(description = "기록 시간")
    private float time;

    @Schema(description = "팀")
    private String team;

    @Schema(description = "메모")
    private String memo;
}
