package com.fitian.burntz.domain.classes.repository;

import com.fitian.burntz.domain.classes.entity.ClassParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.classes.repository
 * @fileName : ClassParticipantRepository
 * @date : 2025-09-15
 * @description : 수업 참여자 리포지토리입니다.
 */
public interface ClassParticipantRepository extends JpaRepository<ClassParticipant, Long> {
}
