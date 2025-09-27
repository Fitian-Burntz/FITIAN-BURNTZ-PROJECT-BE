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
@Schema(name = "RecordResponse", description = "레코드 반환 DTO")
public class RecordResponse {

    @Schema(description = "순위", example = "1")
    private int rank;

    @Schema(description = "member Pk", example = "34")
    private Long memberPk;
    
    @Schema(description = "record Pk", example = "100")
    private Long recordPk;

    @Schema(description = "wod Pk", example = "1")
    private Long wodPk;

    @Schema(description = "Classes Pk", example = "1")
    private Long classesPk;
    
    @Schema(description = "nickname", example = "길동")
    private String nickname;
    
    @Schema(description = "단계", example = "Rx'd")
    private String level;
    
    @Schema(description = "라운드", example = "3")
    private Integer round;
    
    @Schema(description = "렙스", example = "5")
    private Integer reps;
    
    @Schema(description = "기록 시간", example = "185.33")
    private Float time;

    @Schema(description = "팀", example = "3efd34tgd")
    private String team;

    @Schema(description = "메모", example = "자세 좋음")
    private String memo;

    public static RecordResponse from(Record r) {
        if (r == null) return null;

        Long memberPk = (r.getMemberList() != null) ? r.getMemberList().getMemberListPk() : null;

        return RecordResponse.builder()
                .rank(0)
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

    /**
     * rank를 지정해서 DTO 생성할 때 사용.
     * RankingRow.getRank() 값을 그대로 넣어서 반환하려면 이 메서드를 사용하면 됨.
     */
    public static RecordResponse fromWithRank(Record r, int rank) {
        RecordResponse resp = from(r);
        resp.rank = rank;
        return resp;
    }
}
