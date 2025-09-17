package com.fitian.burntz.domain.record.repository;

import com.fitian.burntz.domain.record.entity.Record;
import com.fitian.burntz.global.common.entity.BaseTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

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
}
