package com.fitian.burntz.domain.membership.repository;

import com.fitian.burntz.domain.membership.entity.Membership;
import com.fitian.burntz.domain.membership.enums.MembershipStatus;
import com.fitian.burntz.global.common.entity.BaseTime;
import com.fitian.burntz.global.common.entity.BaseTime.Yn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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

    List<Membership> findByBoxBoxPkAndMemberMemberPk(Long boxPk, Long memberPk);

    /** 해당 box 에 member 의 활성화 membershipPk 중 가장 큰 값 조회 **/
    @Query(value =
            "SELECT DISTINCT ON (m.member_pk) m.* " +
                    "FROM burntz.membership m " +   // <-- 스키마 명시
                    "WHERE m.box_pk = :boxPk AND m.member_pk IN (:memberPks) AND m.deleted_yn = 'N' " +
                    "ORDER BY m.member_pk, m.membership_pk DESC",
            nativeQuery = true)
    List<Membership> findLatestMembershipPerMemberByBox(@Param("boxPk") Long boxPk,
                                                        @Param("memberPks") List<Long> memberPks);

    /** 해당 box 에 member의 활성화 membership 중 가장 큰 값 단건 조회용 **/
    @Query("""
        SELECT m
        FROM Membership m
        WHERE m.box.boxPk = :boxPk
          AND m.member.memberPk = :memberPk
          AND m.deletedYN = 'N'
        ORDER BY m.membershipPk DESC
        """)
    List<Membership> findAllMembershipByBoxPkAndMemberPk(@Param("boxPk") Long boxPk,
                                                      @Param("memberPk") Long memberPk);

    /** box 별 사용자의 최신 membership 을 한번에 가져옴
     * 사용자가 내가 속한 box 정보를 조회할 때 **/
    @Query(value = """
    SELECT DISTINCT ON (m.box_pk) m.*
    FROM burntz.membership m
    WHERE m.box_pk IN (:boxPks)
      AND m.member_pk = :memberPk
      AND m.deleted_yn = 'N'
    ORDER BY m.box_pk, m.membership_pk DESC
    """, nativeQuery = true)
    List<Membership> findLatestMembershipsForMemberByBoxes(@Param("boxPks") List<Long> boxPks,
                                                           @Param("memberPk") Long memberPk);

    /** 만료 기일이 지난 membership을 한번에 가져옴 **/
    List<Membership> findAllByExpirationDateLessThanAndStatusAndDeletedYN(
            LocalDate today,
            MembershipStatus status,
            BaseTime.Yn deletedYN
    );

    /** 멤버십 존재하는지 체크 **/
    boolean existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(
            Long boxPk,
            Long memberPk,
            BaseTime.Yn deletedYN
    );

    /** 활성 멤버십 존재하는지 체크 (만료/삭제 제외) **/
    boolean existsByBoxBoxPkAndMemberMemberPkAndDeletedYNAndStatus(
            Long boxPk,
            Long memberPk,
            BaseTime.Yn deletedYN,
            MembershipStatus status
    );
}
