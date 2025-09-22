package com.fitian.burntz.domain.classes.repository;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.classes.entity.Classes;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.common.entity.BaseTime.Yn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.classes.repository
 * @fileName : ClassesRepository
 * @date : 2025-09-15
 * @description : 수업 리포지토리입니다.
 */
public interface ClassesRepository extends JpaRepository<Classes, Long> {

    List<Classes> findByBoxAndClassDateBetweenAndDeletedYN(
            Box box, LocalDate startDate, LocalDate endDate, Yn deletedYN);

    List<Classes> findByBoxBoxPkAndClassDateBetweenAndDeletedYN(
            Long boxPk, LocalDate startDate, LocalDate endDate, Yn deletedYN);

    //Class 존재 여부 확인 및 소속 검증(해당 box에 소속되어있는지)
     Optional<Classes> findByClassesPkAndBoxBoxPkAndDeletedYN(Long classesPk, Long boxPk, BaseTime.Yn deletedYN) ;
}
