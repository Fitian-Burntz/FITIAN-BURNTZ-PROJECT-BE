package com.fitian.burntz.domain.record.repository;

import com.fitian.burntz.domain.record.entity.Record;
import com.fitian.burntz.domain.wod.entity.Wod;
import com.fitian.burntz.global.common.entity.BaseTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * @author : 선순주
 * @packageName : com.fitian.burntz.domain.record.repository
 * @fileName : RecordRepository
 * @date : 2025-09-17
 * @description : Record Repository
 */

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {

    //회원(memberPk)를 기준으로 해당 Classes에 이미 운동 기록이 있는지 확인
    boolean existsByClassesClassesPkAndMemberMemberPkAndDeletedYN(Long classesPk, Long memberPk, BaseTime.Yn deletedYN);

    //해당 날짜의 전체 record 반환
    @Query("select r from Record r left join fetch r.member left join fetch r.classes where r.wod = :wod and r.deletedYN = 'N'")
    List<Record> findAllByWodWithMemberAndClasses(@Param("wod") Wod wod, @Param("deletedYN") BaseTime.Yn deletedYN);

    //해당 레코드 존재여부 확인(wod,box 소속)
    @Query("select r from Record r " +
            "join fetch r.wod w " +
            "join fetch w.box b " +
            "where r.recordPk = :recordPk")
    Optional<Record> findByIdWithWodAndBox(@Param("recordPk") Long recordPk);
}
