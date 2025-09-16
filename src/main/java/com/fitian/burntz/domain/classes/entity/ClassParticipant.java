package com.fitian.burntz.domain.classes.entity;

import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.classes.entity
 * @fileName : ClassParticipant
 * @date : 2025-09-04
 * @description : 수업참여자 entity
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "class_participant")
public class ClassParticipant extends BaseTime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "class_participant_pk")
    private Long classParticipantPk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classes_pk", nullable = false)
    private Classes classes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_pk", nullable = false)
    private Member member;

}