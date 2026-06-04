package com.fitian.burntz.domain.wod.repository;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.wod.entity.Wod;
import com.fitian.burntz.global.common.entity.BaseTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WodRepository extends JpaRepository<Wod, Long> {
    //특정 날짜에 해당하는 wod 조회(deleteYN이 N인것만)
    Optional<Wod> findByBoxAndWodDateAndDeletedYN(Box box, LocalDate wodDate, BaseTime.Yn deletedYN);

    //해당 날짜의 wod 존재 여부 확인
    boolean existsByBoxAndWodDateAndDeletedYN(Box box, LocalDate wodDate, BaseTime.Yn deletedYN);

    //wod 존재 및 소속 검증(wod가 box에 속하는지)
    Optional<Wod> findByWodPkAndBoxBoxPkAndDeletedYN(Long wodPk, Long boxPk, BaseTime.Yn deletedYN);

    //wod 존재 및 소속 검증 (box+date로 조회)
    Optional<Wod> findByBoxBoxPkAndWodDateAndDeletedYN(Long boxPk, LocalDate date, BaseTime.Yn deletedYN);

    List<Wod> findByBoxBoxPkAndWodDateBetweenAndDeletedYNOrderByWodDateDesc(
            Long boxPk, LocalDate startDate, LocalDate endDate, BaseTime.Yn deletedYN);
}
