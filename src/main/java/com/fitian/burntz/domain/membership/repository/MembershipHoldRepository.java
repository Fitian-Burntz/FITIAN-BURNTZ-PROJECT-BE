package com.fitian.burntz.domain.membership.repository;

import com.fitian.burntz.domain.membership.entity.MembershipHold;
import com.fitian.burntz.domain.membership.enums.HoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MembershipHoldRepository extends JpaRepository<MembershipHold, Long> {

    List<MembershipHold> findAllByMembershipMembershipPkOrderByHoldStartDateDesc(Long membershipPk);

    // 해당 멤버십의 SCHEDULED/ACTIVE 홀딩 중 날짜 겹치는 것 조회 (겹침 검증용)
    @Query("SELECT h FROM MembershipHold h WHERE h.membership.membershipPk = :membershipPk " +
            "AND h.status IN ('SCHEDULED', 'ACTIVE') " +
            "AND h.holdStartDate <= :endDate AND h.holdEndDate >= :startDate")
    List<MembershipHold> findOverlapping(@Param("membershipPk") Long membershipPk,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    // 스케줄러: 오늘 활성화할 SCHEDULED 홀딩
    List<MembershipHold> findAllByStatusAndHoldStartDateLessThanEqual(HoldStatus status, LocalDate date);

    // 스케줄러: 오늘 완료할 ACTIVE 홀딩
    List<MembershipHold> findAllByStatusAndHoldEndDateLessThan(HoldStatus status, LocalDate date);

    // 정책 검증용: SCHEDULED/ACTIVE/COMPLETED 홀딩 목록 (Java에서 일수 합산)
    List<MembershipHold> findAllByMembershipMembershipPkAndStatusIn(Long membershipPk, List<HoldStatus> statuses);

    // 특정 날짜에 SCHEDULED or ACTIVE인 멤버십 홀딩 (수업 신청 차단용)
    @Query("SELECT h FROM MembershipHold h WHERE h.membership.membershipPk = :membershipPk " +
            "AND h.status IN ('SCHEDULED', 'ACTIVE') " +
            "AND h.holdStartDate <= :classDate AND h.holdEndDate >= :classDate")
    Optional<MembershipHold> findActiveOrScheduledOnDate(@Param("membershipPk") Long membershipPk,
                                                          @Param("classDate") LocalDate classDate);
}
