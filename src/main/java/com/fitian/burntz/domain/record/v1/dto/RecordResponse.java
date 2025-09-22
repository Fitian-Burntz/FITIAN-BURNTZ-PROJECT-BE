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

    public static RecordResponse from(Record r){
        if(r == null) return null;

        // 안전한 member 참조
        String displayNickname = null;
        Long memberPk = null;

        // record에 저장된 nickname(생성 시 memberList.nickname으로 채워졌다면 이것을 우선 사용)
        // 닉네임 우선순위: record.nickname -> member.nickname -> null
        if (r.getNickname() != null && !r.getNickname().isBlank()) {
            displayNickname = r.getNickname();
        } else if (r.getMember() != null) {
            // record.nickname 없으면 member 엔티티 닉네임 사용 (회원)
            memberPk = r.getMember().getMemberPk();
            displayNickname = r.getMember().getNickname();
        }

        // 만약 member가 있고 memberPk를 설정하지 않았던 경우(위에서 record.nickname이 먼저 설정된 경우),
        // memberPk는 member가 존재하면 채워주는게 좋음.
        if (memberPk == null && r.getMember() != null) {
            memberPk = r.getMember().getMemberPk();
        }

        return RecordResponse.builder()
                .memberPk(memberPk)
                .recordPk(r.getRecordPk())
                .wodPk(r.getWod() != null ? r.getWod().getWodPk() : null)
                .classesPk(r.getClasses() != null ? r.getClasses().getClassesPk() : null)
                .nickname(displayNickname)
                .level(r.getLevel())
                .round(r.getRound())
                .reps(r.getReps())
                .time(r.getTime())
                .team(r.getTeam())
                .memo(r.getMemo())
                .build();
    }
}
