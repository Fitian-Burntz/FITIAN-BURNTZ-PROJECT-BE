package com.fitian.burntz.domain.locker.repository;

import com.fitian.burntz.domain.locker.entity.Locker;
import com.fitian.burntz.global.common.entity.BaseTime.Yn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LockerRepository extends JpaRepository<Locker, Long> {

    List<Locker> findAllByBoxBoxPkAndDeletedYN(Long boxPk, Yn deletedYN);

    Optional<Locker> findByLockerPkAndBoxBoxPkAndDeletedYN(Long lockerPk, Long boxPk, Yn deletedYN);

    boolean existsByBoxBoxPkAndLockerNumberAndDeletedYN(Long boxPk, String lockerNumber, Yn deletedYN);
}
