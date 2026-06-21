package com.fitian.burntz.domain.membership.entity;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.membership.enums.MembershipStatus;
import com.fitian.burntz.domain.membership.v1.dto.MembershipUpdateRequest;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.membership.entity
 * @fileName : Membership
 * @date : 2025-09-04
 * @description : Membership 엔티티
 */

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "membership")
public class Membership extends BaseTime {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "membership_pk")
    private Long membershipPk;

    @Column(name = "membership_name", length = 100)
    private String membershipName;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "expiration_date")
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 100)
    private MembershipStatus status;

    @Column(name = "memo", length = 255)
    private String memo;

    // null이면 박스 정책(BoxHoldingPolicy) 기준, 값이 있으면 이 회원에게만 적용되는 커스텀 한도
    @Column(name = "custom_max_hold_days")
    private Integer customMaxHoldDays;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_pk", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_pk", nullable = false)
    private Box box;

    public void updateFrom(MembershipUpdateRequest req) {
        if (req == null) return;
        //  startDate  expirationDate   status  memo  period

        if (req.getMembershipName() != null && !req.getMembershipName().isBlank()) {
            this.membershipName = req.getMembershipName();
        }

        if (req.getStartDate() != null) {
            this.startDate = req.getStartDate();
        }

        if (req.getExpirationDate() != null) {
            this.expirationDate = req.getExpirationDate();
        }

        if (req.getStatus() != null) {
            this.status = req.getStatus();
        }

        if (req.getMemo() != null && !req.getMemo().isBlank()) {
            this.memo = req.getMemo();
        }
    }

    public void startHolding() {
        this.status = MembershipStatus.HOLDING;
    }

    public void resumeActive(LocalDate newExpirationDate) {
        this.status = MembershipStatus.ACTIVE;
        this.expirationDate = newExpirationDate;
    }

    public void expire() {
        if (this.status != MembershipStatus.EXPIRED) {
            this.status = MembershipStatus.EXPIRED;
        }
    }

    public void delete() {
        if (this.status != MembershipStatus.DELETE) {
            this.status = MembershipStatus.DELETE;
        }
    }
}