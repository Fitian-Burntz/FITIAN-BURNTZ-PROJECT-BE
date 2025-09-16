package com.fitian.burntz.domain.classes.repository;

import com.fitian.burntz.domain.classes.entity.ClassParticipant;
import com.fitian.burntz.domain.classes.v1.dto.ClassParticipantResponse;
import com.fitian.burntz.global.common.entity.BaseTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    boolean existsByClassesClassesPkAndMemberMemberPkAndDeletedYN(Long classesPk, Long memberPk, BaseTime.Yn deletedYN);
    Optional<ClassParticipant> findByClassesClassesPkAndMemberMemberPkAndDeletedYN(Long classesPk, Long memberPk, BaseTime.Yn deletedYN);

    @Query("SELECT new com.fitian.burntz.domain.classes.v1.dto.ClassParticipantResponse("
            + " cp.classParticipantPk, cp.classesPk, cp.memberPk, cp.createdAt ) "
            + "FROM ClassParticipant cp "
            + "WHERE cp.classesPk = :classesPk AND cp.deletedYN = :deletedYN")
    List<ClassParticipantResponse> findResponsesByClassesPkAndDeletedYN(
            @Param("classesPk") Long classesPk,
            @Param("deletedYN") BaseTime.Yn deletedYN);

    List<ClassParticipant> findByClassesClassesPkAndDeletedYN(Long classesPk, BaseTime.Yn deletedYN);
}
