package com.fitian.burntz.domain.membership.repository;

import com.fitian.burntz.domain.membership.entity.Membership;
import com.fitian.burntz.global.common.entity.BaseTime.Yn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    // 해당 box 에 member 의 활성화 membershipPk 중 가장 큰 값 조회
    @Query(value =
            "SELECT DISTINCT ON (m.member_pk) m.* " +
                    "FROM burntz.membership m " +   // <-- 스키마 명시
                    "WHERE m.box_pk = :boxPk AND m.member_pk IN (:memberPks) AND m.deleted_yn = 'N' " +
                    "ORDER BY m.member_pk, m.membership_pk DESC",
            nativeQuery = true)
    List<Membership> findLatestMembershipPerMemberByBox(@Param("boxPk") Long boxPk,
                                                        @Param("memberPks") List<Long> memberPks);
}
