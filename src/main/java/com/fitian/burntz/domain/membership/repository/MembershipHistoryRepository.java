package com.fitian.burntz.domain.membership.repository;

import com.fitian.burntz.domain.membership.entity.MembershipHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.membership.repository
 * @fileName : MembershipHistoryRepository
 * @date : 2025-09-18
 * @description : 멤버십 로그 리포지토리입니다
 */
public interface MembershipHistoryRepository extends JpaRepository<MembershipHistory, Long> {
    List<MembershipHistory> findAllByMembershipMembershipPk(Long MembershipPk);
}
