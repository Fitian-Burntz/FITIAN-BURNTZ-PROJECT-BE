package com.fitian.burntz.domain.record.v1.dto;

import com.fitian.burntz.domain.classes.entity.Classes;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.record.entity.Record;
import com.fitian.burntz.domain.record.enums.RecordResult;
import com.fitian.burntz.domain.wod.entity.Wod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
    private Long memberListPk;

    @Schema(description = "비회원일 경우 닉네임(memberPk가 없을 때 필수)")
    private String nickname;

    @NotNull
    @Schema(description = "클래스 pk")
    private Long classesPk;

    @Schema(description = "단계")
    private String level;

    @Schema(description = "라운드")
    private Integer round;

    @Schema(description = "렙스")
    private Integer reps;

    @Schema(description = "기록 시간")
    private Float time;

    @Schema(description = "운동 결과")
    private RecordResult result;

    @Schema(description = "팀")
    private String team;

    @Schema(description = "메모")
    private String memo;

    // memberList에서 가져온 nickname을 인자로 받음
    public Record toEntity(Wod wod, Classes classes, MemberList memberList, String nicknameFromMemberList){
        Record.RecordBuilder builder = Record.builder()
                .wod(wod)
                .classes(classes);

        // member가 있으면 연관 설정
        if (memberList != null && memberList.getMemberListPk() != null) builder.memberList(memberList);

        // 닉네임 우선순위: memberList에서 준 nicknameFromMemberList가 있으면 그걸 사용.
        if (nicknameFromMemberList != null) {
            builder.nickname(nicknameFromMemberList);
        } else if (this.nickname != null) {
            // 회원이 아닌 경우(또는 memberList 닉네임이 없는 경우) 클라이언트가 보낸 nickname 사용
            builder.nickname(this.nickname);
        }

        if (this.level != null)    builder.level(this.level);
        if (this.round != null)    builder.round(this.round);
        if (this.reps != null)     builder.reps(this.reps);
        if (this.result != null)   builder.result(this.result);
        if (this.time != null)      builder.time(this.time);
        if (this.team != null)     builder.team(this.team);
        if (this.memo != null)     builder.memo(this.memo);

        return builder.build();
    }

}