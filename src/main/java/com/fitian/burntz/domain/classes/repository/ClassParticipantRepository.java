package com.fitian.burntz.domain.classes.repository;

import com.fitian.burntz.domain.classes.entity.ClassParticipant;
import com.fitian.burntz.global.common.entity.BaseTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.classes.repository
 * @fileName : ClassParticipantRepository
 * @date : 2025-09-15
 * @description : 수업 참여자 리포지토리입니다.
 */
public interface ClassParticipantRepository extends JpaRepository<ClassParticipant, Long> {
    boolean existsByClassesClassesPkAndMemberListMemberMemberPkAndDeletedYN(Long classesPk, Long memberPk, BaseTime.Yn deletedYN);
    int countByClassesClassesPkAndDeletedYN(Long classesPk, BaseTime.Yn deletedYN);
    Optional<ClassParticipant> findByClassesClassesPkAndMemberListMemberMemberPkAndDeletedYN(Long classesPk, Long memberPk, BaseTime.Yn deletedYN);

//    @Query("SELECT new com.fitian.burntz.domain.classes.v1.dto.ClassParticipantResponse("
//            + " cp.classParticipantPk, cp.classes.classesPk, cp.member_list.memberPk, cp.createdAt ) "
//            + "FROM ClassParticipant cp "
//            + "WHERE cp.classes.classesPk = :classesPk AND cp.deletedYN = :deletedYN")
//    List<ClassParticipantResponse> findResponsesByClassesPkAndDeletedYN(
//            @Param("classesPk") Long classesPk,
//            @Param("deletedYN") BaseTime.Yn deletedYN);

    List<ClassParticipant> findByClassesClassesPkAndDeletedYN(Long classesPk, BaseTime.Yn deletedYN);

    @Query("SELECT cp FROM ClassParticipant cp JOIN FETCH cp.memberList ml JOIN FETCH ml.member WHERE cp.classes.classesPk = :classesPk AND cp.deletedYN = :deletedYN")
    List<ClassParticipant> findByClassesPkWithMemberList(@Param("classesPk") Long classesPk, @Param("deletedYN") BaseTime.Yn deletedYN);

    List<ClassParticipant> findByClassesClassesPkInAndDeletedYN(List<Long> classesPks, BaseTime.Yn deletedYN);

    @Query("SELECT cp.classes.classesPk FROM ClassParticipant cp " +
            "WHERE cp.classes.classesPk IN :classesPks " +
            "  AND cp.memberList.memberListPk = :memberListPk " +
            "  AND cp.deletedYN = :deletedYN")
    List<Long> findClassesPkByClassesPkInAndMemberListMemberListPkAndDeletedYN(
            @Param("classesPks") Collection<Long> classesPks,
            @Param("memberListPk") Long memberListPk,
            @Param("deletedYN") BaseTime.Yn deletedYN);

    // 홀딩 시작 시 해당 기간 수업 신청 일괄 취소용
    @Query("SELECT cp FROM ClassParticipant cp " +
            "WHERE cp.memberList.member.memberPk = :memberPk " +
            "AND cp.classes.box.boxPk = :boxPk " +
            "AND cp.classes.classDate >= :startDate " +
            "AND cp.classes.classDate <= :endDate " +
            "AND cp.deletedYN = 'N'")
    List<ClassParticipant> findActiveByMemberAndBoxAndDateRange(
            @Param("memberPk") Long memberPk,
            @Param("boxPk") Long boxPk,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("DELETE FROM ClassParticipant cp WHERE cp.classes.classesPk IN (SELECT c.classesPk FROM Classes c WHERE c.box.boxPk = :boxPk)")
    void deleteAllByBoxPk(@Param("boxPk") Long boxPk);
}
