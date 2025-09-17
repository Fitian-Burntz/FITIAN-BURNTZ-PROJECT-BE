package com.fitian.burntz.domain.record.v1.dto;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.classes.entity.Classes;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.record.entity.Record;
import com.fitian.burntz.domain.record.enums.RecordResult;
import com.fitian.burntz.domain.wod.entity.Wod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.record.v1.dto
 * @fileName : RecordCreateRequest
 * @date : 2025-09-17
 * @description : Record 생성용 DTO
 */

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Record Create DTO")
public class RecordCreateRequest {
    @Schema(description = "기록 대상자(회원일 경우)")
    private Long memberPk;

    @Schema(description = "비회원일 경우 닉네임(memberPk가 없을 때 필수)")
    private String nickname;

    @Schema(description = "단계")
    private String level;

    @Schema(description = "라운드")
    private Integer round;

    @Schema(description = "렙스")
    private Integer reps;

    @Schema(description = "기록 시간")
    private float time;

    @Schema(description = "운동 결과")
    private RecordResult result;

    @Schema(description = "팀")
    private String team;

    @Schema(description = "메모")
    private String memo;

    public Record toEntity(Wod wod, Classes classes, Member member){
        Record.RecordBuilder builder = Record.builder()
                .wod(wod)
                .classes(classes);
        //NPE 방지 NULL 조건부 처리
        if (member != null && member.getMemberPk() != null) builder.member(member);
        if(this.nickname != null)   builder.nickname(this.nickname);
        if (this.level != null)    builder.level(this.level);
        if (this.round != null)    builder.round(this.round);
        if (this.reps != null)     builder.reps(this.reps);
        if (this.result != null)   builder.result(this.result);
        if (this.team != null)     builder.team(this.team);
        if (this.memo != null)     builder.memo(this.memo);

        return builder.build();
    }

}