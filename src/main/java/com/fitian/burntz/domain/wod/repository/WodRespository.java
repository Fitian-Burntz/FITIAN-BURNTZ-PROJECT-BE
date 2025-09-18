package com.fitian.burntz.domain.wod.repository;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.wod.entity.Wod;
import com.fitian.burntz.global.common.entity.BaseTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.wod.repository
 * @fileName : WodRespository
 * @date : 2025-09-16
 * @description : Wod 레포지토리
 */
@Repository
public interface WodRespository extends JpaRepository<Wod, Long> {
    //특정 날짜에 해당하는 wod 조회(deleteYN이 N인것만)
    Optional<Wod> findByBoxAndWodDateAndDeletedYN(Box box, LocalDate wodDate, BaseTime.Yn deletedYN);

    //해당 날짜의 wod 존재 여부 확인
    boolean existsByBoxAndWodDateAndDeletedYN(Box box, LocalDate wodDate, BaseTime.Yn deletedYN);


 }
