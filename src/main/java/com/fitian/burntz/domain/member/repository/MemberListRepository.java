package com.fitian.burntz.domain.member.repository;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.global.common.entity.BaseTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
    Optional<MemberRole> findRoleByMemberAndBoxAndDeletedYN(Member member, Box box, BaseTime.Yn deletedYN);

    //해당 box에 해당 멤버가 속해있는지 확인하고 MemberList값을 반환
    Optional<MemberList> findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(Long memberPk, Long boxPk, BaseTime.Yn deletedYN);


    boolean existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(Long boxPk, Long memberPk, BaseTime.Yn deletedYN);


    @Query("SELECT m FROM MemberList m WHERE m.box.boxPk = :boxPk AND m.member.memberPk = :memberPk AND m.deletedYN = 'N'")
    Optional<MemberList> findActiveByBoxPkAndMemberPk(@Param("boxPk") Long boxPk, @Param("memberPk") Long memberPk);


    // memberList ABC 순으로 정렬해서 조회
    @Query(
            value = "SELECT ml FROM MemberList ml JOIN FETCH ml.member m " +
                    "WHERE ml.box.boxPk = :boxPk AND ml.deletedYN = 'N' " +
                    "ORDER BY m.nickname ASC",   // <-- 대소문자 구분은 DB collation에 따름
            countQuery = "SELECT COUNT(ml) FROM MemberList ml WHERE ml.box.boxPk = :boxPk AND ml.deletedYN = 'N'"
    )
    Page<MemberList> findActiveByBoxPkWithMember(@Param("boxPk") Long boxPk, Pageable pageable);

    // memberList 에서 삭제되지 않은 행 중 중복 데이터가 있는지 확인
    @Query(value = "select exists (select 1 from burntz.member_list where box_pk = :boxPk and member_pk = :memberPk and deleted_yn = 'N')",
            nativeQuery = true)
    boolean existsActiveByBoxPkAndMemberPk(@Param("boxPk") Long boxPk, @Param("memberPk") Long memberPk);

    long countByBox_BoxPkAndRole(Long boxPk, MemberRole role);

    Optional<MemberList> findByMemberListPkAndBoxBoxPkAndDeletedYN(Long memberListPk, Long boxPk, BaseTime.Yn yn);
}
