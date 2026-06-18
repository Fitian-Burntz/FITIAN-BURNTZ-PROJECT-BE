package com.fitian.burntz.domain.locker.entity;

import com.fitian.burntz.domain.locker.enums.LockerUsageStatus;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "locker_usage")
public class LockerUsage extends BaseTime {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "locker_usage_pk")
    private Long lockerUsagePk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "locker_pk", nullable = false)
    private Locker locker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_list_pk", nullable = false)
    private MemberList memberList;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private LockerUsageStatus status;

    public void expire() {
        this.status = LockerUsageStatus.EXPIRED;
    }
}
