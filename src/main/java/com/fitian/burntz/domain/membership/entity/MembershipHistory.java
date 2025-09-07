package com.fitian.burntz.domain.membership.entity;

import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.membership.enums.HistoryActionType;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.membership.entity
 * @fileName : MembershipHistory
 * @date : 2025-09-04
 * @description : 멤버쉽 로그 entity
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "membership_history")
public class MembershipHistory extends BaseTime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_history_pk")
    private Long membershipHistoryPk;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", length = 50)
    private HistoryActionType actionType;

    @Column(name = "pre_value",columnDefinition="text")
    private String preValue;

    @Column(name = "new_value",columnDefinition="text")
    private String newValue;

    @Column(name = "memo",columnDefinition="text")
    private String memo;

    @Column(name = "period")
    private Integer period;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_pk", nullable = false)
    private Membership membership;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Member createdBy;
}