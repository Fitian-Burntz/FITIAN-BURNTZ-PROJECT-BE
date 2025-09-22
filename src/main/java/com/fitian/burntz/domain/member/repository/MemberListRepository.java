package com.fitian.burntz.domain.member.repository;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.global.common.entity.BaseTime;
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

    //해당 box에 해당 멤버가 속해있는지 확인
    boolean existsByMemberMemberPkAndBoxBoxPkAndDeletedYN(Long memberPk, Long boxPk, BaseTime.Yn deletedYN);

    //특정 박스에 속한 MemberList 엔티티를 반환
    Optional<MemberList> findByMemberMemberPkAndBoxBoxPkAndDeletedYN(Long memberPk, Long boxPk, BaseTime.Yn deletedYN);

    //MemberPks 목록에 포함된 회원들에 대해 특정 box에 속해있는 MemberList 들 가져오는 메서드
    List<MemberList> findAllByMemberMemberPkInAndBoxBoxPkAndDeletedYN(List<Long> memberPks, Long boxPk, BaseTime.Yn deletedYN);

    boolean existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(Long boxPk, Long memberPk, BaseTime.Yn deletedYN);


    Optional<MemberList> findByBox_BoxPkAndMember_MemberPk(Long boxPk, Long memberPk);

    // memberList 에서 삭제되지 않은 행 중 중복 데이터가 있는지 확인
    @Query(value = "select exists (select 1 from burntz.member_list where box_pk = :boxPk and member_pk = :memberPk and deleted_yn = 'N')",
            nativeQuery = true)
    boolean existsActiveByBoxPkAndMemberPk(@Param("boxPk") Long boxPk, @Param("memberPk") Long memberPk);

    long countByBox_BoxPkAndRole(Long boxPk, MemberRole role);
}
