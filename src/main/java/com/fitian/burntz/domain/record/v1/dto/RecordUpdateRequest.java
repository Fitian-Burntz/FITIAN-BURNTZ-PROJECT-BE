package com.fitian.burntz.domain.record.v1.dto;

import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.record.enums.RecordResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.record.v1.dto
 * @fileName : RecordUpdateRequest
 * @date : 2025-09-18
 * @description : Record 수정용 dto
 */

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecordUpdateRequest {

    /**
     * 최종 반영할 기록자 memberPk (null => 비회원)
     */
    @Schema(description = "기록 대상자(회원일 경우)")
    private Long memberPk;

    /**
     * 비회원 닉네임 (memberPk가 null일 때 사용).
     * null => 변경 없음 (부분수정)
     */
    @Schema(description = "비회원일 경우 닉네임(memberPk가 없을 때 필수)")
    private String nickname;

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



    /**
     * Service에서 targetMember(존재하면 회원, null이면 비회원 or 변경 없음 의도)와
     * nicknameToSet(명시적 닉네임 변경 값; null이면 "변경 없음")을 계산 후 호출.
     *
     * nicknameToSet 의미:
     *  - null : 닉네임 변경 없음
     *  - non-null : 해당 값으로 nickname을 설정(비회원 전환 포함)
     *
     * targetMember 의미:
     *  - non-null : 해당 Member로 연관 설정, nickname은 targetMember.getNickname()으로 덮어씀
     *  - null & nicknameToSet != null : member 연관 제거(비회원) & nicknameToSet 적용
     *  - null & nicknameToSet == null : member/nickname 변경 없음
     */
    public void applyTo(Record record, Member targetMember, String nicknameToSet) {
        record.updateByAdmin(
                targetMember,
                nicknameToSet,
                this.level,
                this.round,
                this.reps,
                this.time,
                this.result,
                this.team,
                this.memo
        );
    }
}