package com.fitian.burntz.domain.member.repository;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.domain.membership.entity.Membership;
import com.fitian.burntz.global.common.entity.BaseTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.member.repository
 * @fileName : MemberListRepository
 * @date : 2025-09-11
 * @description : 박스 내 멤버 리스트 리포지토리입니다.
 */
public interface MemberListRepository extends JpaRepository<MemberList, Long> {

    /** 활성화 MemberList 중 해당 memberListPk 조회 **/
    @Query("SELECT m FROM MemberList m WHERE m.memberListPk = :memberListPk AND m.deletedYN = 'N'")
    Optional<MemberList> findActiveById(@Param("memberListPk") Long memberListPk);

    Optional<MemberRole> findRoleByMemberAndBoxAndDeletedYN(Member member, Box box, BaseTime.Yn deletedYN);

    //해당 box에 해당 멤버가 속해있는지 확인하고 MemberList값을 반환
    Optional<MemberList> findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(Long memberPk, Long boxPk, BaseTime.Yn deletedYN);


    boolean existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(Long boxPk, Long memberPk, BaseTime.Yn deletedYN);


    @Query("SELECT m FROM MemberList m WHERE m.box.boxPk = :boxPk AND m.member.memberPk = :memberPk AND m.deletedYN = 'N'")
    Optional<MemberList> findActiveByBoxPkAndMemberPk(@Param("boxPk") Long boxPk, @Param("memberPk") Long memberPk);


    // box에 해당하는 memberList 페이징 조회
    @Query(
            value = "SELECT ml FROM MemberList ml JOIN FETCH ml.member m " +
                    "WHERE ml.box.boxPk = :boxPk AND ml.deletedYN = 'N'",
            countQuery = "SELECT COUNT(ml) FROM MemberList ml WHERE ml.box.boxPk = :boxPk AND ml.deletedYN = 'N'"
    )
    Page<MemberList> findActiveByBoxPkWithMember(@Param("boxPk") Long boxPk, Pageable pageable);

    // memberList 에서 삭제되지 않은 행 중 중복 데이터가 있는지 확인
    @Query(value = "select exists (select 1 from burntz.member_list where box_pk = :boxPk and member_pk = :memberPk and deleted_yn = 'N')",
            nativeQuery = true)
    boolean existsActiveByBoxPkAndMemberPk(@Param("boxPk") Long boxPk, @Param("memberPk") Long memberPk);


    Optional<MemberList> findByMemberListPkAndBoxBoxPkAndDeletedYN(Long memberListPk, Long boxPk, BaseTime.Yn yn);


    /** update 시 경생 상황을 피하기 위해서 row lock 을 걸고 member 가 box 에 속해있는지 확인 **/
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MemberList m WHERE m.box.boxPk = :boxPk AND m.member.memberPk = :memberPk AND m.deletedYN = 'N'")
    Optional<MemberList> findActiveMemberListByBoxAndMemberWithLock(@Param("boxPk") Long boxPk,
                                                                    @Param("memberPk") Long memberPk);

    /**
     * update 시 경쟁 상황을 피하기 위해서 row lock 을 걸고
     * 해당 활성화 memberList 에서 해당 box의 OWNER row 개수 구함 (OWNER 수 검증) **/
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<MemberList> findTop2ByBox_BoxPkAndRoleAndDeletedYN(Long boxPk, MemberRole role, BaseTime.Yn yn);


    /**
     * memberList + box 페이징
     * 사용자가 내가 속한 box 정보를 조회할 때 **/
    @Query(value = "SELECT ml FROM MemberList ml JOIN FETCH ml.box b " +
            "WHERE ml.member.memberPk = :memberPk AND ml.deletedYN = 'N'",
            countQuery = "SELECT COUNT(ml) FROM MemberList ml WHERE ml.member.memberPk = :memberPk AND ml.deletedYN = 'N'")
    Page<MemberList> findActiveByMemberPkWithBox(@Param("memberPk") Long memberPk, Pageable pageable);

}
