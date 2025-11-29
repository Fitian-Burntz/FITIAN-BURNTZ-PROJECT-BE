package com.fitian.burntz.domain.classes.repository;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.classes.entity.Classes;
import com.fitian.burntz.domain.classes.v1.dto.ClassesWithCountResponse;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.common.entity.BaseTime.Yn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

     @Query("SELECT new com.fitian.burntz.domain.classes.v1.dto.ClassesWithCountResponse(c, COUNT(cp)) " +
     "FROM Classes c " +
     "LEFT JOIN ClassParticipant cp ON cp.classes = c AND cp.deletedYN = :participantDeletedYN " +
     "WHERE c.box.boxPk = :boxPk " +
     " AND c.classDate BETWEEN :startDate AND :endDate " +
     " AND c.deletedYN = :deletedYN " +
     "GROUP BY c " +
     "ORDER BY c.startTime")
     List<ClassesWithCountResponse> findWithParticipantCountByBoxAndDate(
             @Param("boxPk") Long boxPk,
             @Param("startDate") LocalDate startDate,
             @Param("endDate") LocalDate endDate,
             @Param("deletedYN") BaseTime.Yn deletedYN,
             @Param("participantDeletedYN") BaseTime.Yn participantDeletedYN
     );
}
