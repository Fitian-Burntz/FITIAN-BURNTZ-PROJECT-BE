package com.fitian.burntz.domain.locker.repository;

import com.fitian.burntz.domain.locker.entity.LockerUsage;
import com.fitian.burntz.domain.locker.enums.LockerUsageStatus;
import com.fitian.burntz.global.common.entity.BaseTime.Yn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LockerUsageRepository extends JpaRepository<LockerUsage, Long> {

    Optional<LockerUsage> findByLockerLockerPkAndStatusAndDeletedYN(
            Long lockerPk, LockerUsageStatus status, Yn deletedYN);

    Optional<LockerUsage> findByMemberListMemberListPkAndLockerBoxBoxPkAndStatusAndDeletedYN(
            Long memberListPk, Long boxPk, LockerUsageStatus status, Yn deletedYN);

    @Query("SELECT lu FROM LockerUsage lu JOIN FETCH lu.memberList ml " +
           "JOIN lu.locker l WHERE l.box.boxPk = :boxPk " +
           "AND lu.status = :status AND lu.deletedYN = :deletedYN")
    List<LockerUsage> findActiveUsagesByBoxPk(
            @Param("boxPk") Long boxPk,
            @Param("status") LockerUsageStatus status,
            @Param("deletedYN") Yn deletedYN);

    List<LockerUsage> findAllByEndDateBeforeAndStatusAndDeletedYN(
            LocalDate today, LockerUsageStatus status, Yn deletedYN);
}
