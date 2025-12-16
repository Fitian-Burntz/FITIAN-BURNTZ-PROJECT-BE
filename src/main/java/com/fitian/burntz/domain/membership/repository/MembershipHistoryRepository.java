package com.fitian.burntz.domain.membership.repository;

import com.fitian.burntz.domain.membership.entity.MembershipHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
        select mh
        from MembershipHistory mh
        join fetch mh.createdBy cb
        where mh.membership.membershipPk = :membershipPk
        order by mh.createdAt desc
    """)
    List<MembershipHistory> findAllByMembershipPkWithCreator(@Param("membershipPk") Long membershipPk);
}
