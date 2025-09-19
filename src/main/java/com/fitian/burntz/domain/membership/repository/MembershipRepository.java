package com.fitian.burntz.domain.membership.repository;

import com.fitian.burntz.domain.membership.entity.Membership;
import com.fitian.burntz.global.common.entity.BaseTime.Yn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.membership.repository
 * @fileName : MembershipRepository
 * @date : 2025-09-17
 * @description : 멤버십 리포지토리입니다
 */
public interface MembershipRepository extends JpaRepository<Membership, Long> {
    Optional<Membership> findByBoxBoxPkAndMemberMemberPkAndDeletedYN(Long boxPk, Long memberPk, Yn deletedYN);
}
