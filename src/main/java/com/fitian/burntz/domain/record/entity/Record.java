package com.fitian.burntz.domain.record.entity;

import com.fitian.burntz.domain.classes.entity.Classes;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.record.enums.RecordResult;
import com.fitian.burntz.domain.wod.entity.Wod;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.record.entity
 * @fileName : Record
 * @date : 2025-09-04
 * @description : Record 엔티티
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "record")
public class Record extends BaseTime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_pk")
    private Long recordPk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wod_pk", nullable = false)
    private Wod wod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classes_pk", nullable = false)
    private Classes classes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_pk")
    private Member member;

    @Column(name = "nickname", length = 50)
    private String nickname;

    @Column(name = "level", length = 50)
    private String level;

    @Column(name = "round")
    private Integer round;

    @Column(name = "reps")
    private Integer reps;

    @Column(name = "time")
    private Float time;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 10)
    private RecordResult result;

    @Column(name = "team", length = 100)
    private String team;

    @Column(name = "memo", length = 255)
    private String memo;

    /**
     * 관리자용 업데이트 (부분수정 지원)
     *
     * targetMember != null : member 연관을 해당 member로 설정, nickname은 member.getNickname()으로 덮어씀
     * targetMember == null && nicknameParam != null : member 연관 제거(비회원) & nicknameParam 설정
     * nicknameParam == null : nickname 변경 없음
     * other fields: null이면 변경 없음
     */
    public void updateByAdmin(
            MemberList targetMemberList,
            String nicknameParam,
            String level,
            Integer round,
            Integer reps,
            Float time,
            RecordResult result,
            String team,
            String memo
    ) {
        if (targetMemberList != null) {
            this.member = targetMemberList.getMember();
            this.nickname = targetMemberList.getBoxNickname();
        } else {
            if (nicknameParam != null) {
                this.member = null;
                this.nickname = nicknameParam;
            }
            // nicknameParam == null -> 변경 없음
        }

        if (level != null) this.level = level;
        if (round != null) this.round = round;
        if (reps != null) this.reps = reps;
        if (time != null) this.time = time;
        if (result != null) this.result = result;
        if (team != null) this.team = team;
        if (memo != null) this.memo = memo;
    }
}