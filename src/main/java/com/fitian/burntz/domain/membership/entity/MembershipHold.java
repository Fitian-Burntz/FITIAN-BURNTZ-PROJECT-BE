package com.fitian.burntz.domain.membership.entity;

import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.membership.enums.HoldStatus;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "membership_hold")
public class MembershipHold extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "hold_pk")
    private Long holdPk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "membership_pk", nullable = false)
    private Membership membership;

    @Column(name = "hold_start_date", nullable = false)
    private LocalDate holdStartDate;

    @Column(name = "hold_end_date", nullable = false)
    private LocalDate holdEndDate;

    @Column(name = "actual_end_date")
    private LocalDate actualEndDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private HoldStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", nullable = false)
    private Member requestedBy;

    @Column(name = "reason", length = 255)
    private String reason;

    // 롤백/재계산 기준: 홀딩 시작 시점의 멤버십 만료일 스냅샷
    @Column(name = "original_expiration_date", nullable = false)
    private LocalDate originalExpirationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private Member cancelledBy;

    public void activate() {
        this.status = HoldStatus.ACTIVE;
    }

    public void complete(LocalDate actualEndDate) {
        this.status = HoldStatus.COMPLETED;
        this.actualEndDate = actualEndDate;
    }

    public void cancel(Member cancelledBy) {
        this.status = HoldStatus.CANCELLED;
        this.actualEndDate = LocalDate.now();
        this.cancelledBy = cancelledBy;
    }
}
