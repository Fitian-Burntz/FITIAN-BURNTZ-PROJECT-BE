package com.fitian.burntz.domain.record.v1.dto;

import com.fitian.burntz.domain.record.entity.Record;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    private Long recordPk;

    @Schema(description = "wod Pk")
    private Long wodPk;

    @Schema(description = "Classes Pk")
    private Long classesPk;
    
    @Schema(description = "nickname")
    private String nickname;
    
    @Schema(description = "단계")
    private String level;
    
    @Schema(description = "라운드")
    private Integer round;
    
    @Schema(description = "렙스")
    private Integer reps;
    
    @Schema(description = "기록 시간")
    private Float time;

    @Schema(description = "팀")
    private String team;

    @Schema(description = "메모")
    private String memo;

    public static RecordResponse from(Record r) {
        if (r == null) return null;

        Long memberPk = (r.getMemberList() != null) ? r.getMemberList().getMemberListPk() : null;

        return RecordResponse.builder()
                .memberPk(memberPk)
                .recordPk(r.getRecordPk())
                .wodPk(r.getWod() != null ? r.getWod().getWodPk() : null)
                .classesPk(r.getClasses() != null ? r.getClasses().getClassesPk() : null)
                .nickname(r.getNickname())
                .level(r.getLevel())
                .round(r.getRound())
                .reps(r.getReps())
                .time(r.getTime())
                .team(r.getTeam())
                .memo(r.getMemo())
                .build();
    }
}
