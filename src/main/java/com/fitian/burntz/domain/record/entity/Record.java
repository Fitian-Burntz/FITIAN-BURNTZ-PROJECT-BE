package com.fitian.burntz.domain.record.entity;

import com.fitian.burntz.domain.classes.entity.Classes;
import com.fitian.burntz.domain.member.entity.Member;
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
    private float time;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 10)
    private RecordResult result;

    @Column(name = "team", length = 100)
    private String team;

    @Column(name = "memo", length = 255)
    private String memo;
}