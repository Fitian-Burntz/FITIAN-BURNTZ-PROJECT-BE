package com.fitian.burntz.domain.record.repository;

import com.fitian.burntz.domain.record.entity.Record;
import com.fitian.burntz.domain.wod.entity.Wod;
import com.fitian.burntz.global.common.entity.BaseTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
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

    //memberListPk를 기준으로 해당 Classes에 이미 운동 기록이 있는지 확인
    boolean existsByClassesClassesPkAndMemberListMemberListPkAndDeletedYN(Long classesPk, Long memberListPk, BaseTime.Yn deletedYN);

    //해당 날짜의 전체 record 반환
    @Query("select r from Record r left join fetch r.memberList left join fetch r.classes where r.wod = :wod and r.deletedYN = 'N'")
    List<Record> findAllByWodWithMemberAndClasses(@Param("wod") Wod wod, @Param("deletedYN") BaseTime.Yn deletedYN);

    //해당 레코드 존재여부 확인(wod,box 소속)
    @Query("select r from Record r " +
            "join fetch r.wod w " +
            "join fetch w.box b " +
            "where r.recordPk = :recordPk")
    Optional<Record> findByIdWithWodAndBox(@Param("recordPk") Long recordPk);

    //자기 자신(헌재의 recordPk)를 제외하고 중복 운동기록이 존재하는지 확인(운동기록 수정 시 방어선)
    boolean existsByClassesClassesPkAndMemberListMemberListPkAndDeletedYNAndRecordPkNot(
            Long classesPk, Long memberListPk, BaseTime.Yn deletedYN, Long recordPk);

    List<Record> findAllByClassesClassesPkInAndMemberListMemberListPkInAndDeletedYN(
            Collection<Long> classesPks, Collection<Long> memberListPks, BaseTime.Yn deletedYN);

    /*
    * 랭킹 관련 쿼리
    * */

    // ForTime: 레벨 → 시간 빠른순 → 닉네임 오름차순
    @Query("""
      select r from Record r
      join r.wod w
      left join r.memberList ml
      where w.box.boxPk = :boxPk
        and w.wodDate = :date
        and r.deletedYN = 'N'
      order by 
        case lower(r.level) when 'rx''d' then 0 when 'a' then 1 when 'b' then 2 when 'c' then 3 else 9 end,
        r.time asc nulls last,
        lower(coalesce(r.nickname, ml.boxNickname, '')) asc
    """)
    List<Record> findForTimeOrder(Long boxPk, LocalDate date);

    // AMRAP:  레벨 -> 성공(0) -> 실패(1) → 닉네임
    @Query("""
      select r from Record r
      join r.wod w
      left join r.memberList ml
      where w.box.boxPk = :boxPk
        and w.wodDate = :date
        and r.deletedYN = 'N'
      order by 
        case lower(r.level) when 'rx''d' then 0 when 'a' then 1 when 'b' then 2 when 'c' then 3 else 9 end,
        r.round desc nulls last,
        r.reps  desc nulls last,
        lower(coalesce(r.nickname, ml.boxNickname, '')) asc
    """)
    List<Record> findAmrapOrder(Long boxPk, LocalDate date);

    // EMOM / 레벨 -> 성공(0) -> 실패(1) → 닉네임
    @Query("""
      select r from Record r
      join r.wod w
      left join r.memberList ml
      where w.box.boxPk = :boxPk
        and w.wodDate = :date
        and r.deletedYN = 'N'
      order by 
        case lower(r.level) when 'rx''d' then 0 when 'a' then 1 when 'b' then 2 when 'c' then 3 else 9 end,
        case when upper(coalesce(r.result,'F')) = 'S' then 0 else 1 end,
        lower(coalesce(r.nickname, ml.boxNickname, '')) asc
    """)
    List<Record> findEmomOrSfOrder(Long boxPk, LocalDate date);

    // MAXREPS / EMOMMAX: 레벨 → 라운드 많은순 → 렙스 많은순 → 닉네임
    @Query("""
      select r from Record r
      join r.wod w
      left join r.memberList ml
      where w.box.boxPk = :boxPk
        and w.wodDate = :date
        and r.deletedYN = 'N'
      order by 
        case lower(r.level) when 'rx''d' then 0 when 'a' then 1 when 'b' then 2 when 'c' then 3 else 9 end,
        r.reps desc nulls last,
        lower(coalesce(r.nickname, ml.boxNickname, '')) asc
    """)
    List<Record> findMaxRepsOrder(Long boxPk, LocalDate date);

    /** 랭킹 결과(recordPk 목록)를 한 번에 긁어오기 위한 조회.
     *  연관까지 fetch 해서 N+1 방지. DISTINCT로 중복 제거.
     */
    @Query("""
        select distinct r
        from Record r
        left join fetch r.memberList ml
        left join fetch r.classes c
        left join fetch r.wod w
        where r.recordPk in (:ids)
    """)
    List<Record> findAllByRecordPkInWithJoins(List<Long> ids);

    /**
     * 특정 MemberListPk의 모든 활성 Record 조회
     */
    @Query("""
    select r from Record r
    join fetch r.wod w
    join fetch w.box b
    where r.memberList.memberListPk = :memberListPk
      and r.deletedYN = :deletedYN
""")
    List<Record> findAllByMemberListPkAndDeletedYN(
            @Param("memberListPk") Long memberListPk,
            @Param("deletedYN") BaseTime.Yn deletedYN
    );

    List<Record> findByClassesClassesPkAndDeletedYN(Long classesPk, BaseTime.Yn deletedYN);

    List<Record> findByWodWodPkAndDeletedYN(Long wodPk, BaseTime.Yn deletedYN);
}
