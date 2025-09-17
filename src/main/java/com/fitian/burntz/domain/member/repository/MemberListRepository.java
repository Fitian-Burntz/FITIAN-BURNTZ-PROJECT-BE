package com.fitian.burntz.domain.member.repository;

import com.fitian.burntz.domain.box.entity.Box;
import com.fitian.burntz.domain.box.enums.MemberRole;
import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.entity.MemberList;
import com.fitian.burntz.global.common.entity.BaseTime;
import org.springframework.data.jpa.repository.JpaRepository;

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
    Optional<MemberList> findRoleByMemberMemberPkAndBoxBoxPkAndDeletedYN(Long memberPk, Long boxPk, BaseTime.Yn deletedYN);
    boolean existsByBoxBoxPkAndMemberMemberPkAndDeletedYN(Long boxPk, Long memberPk, BaseTime.Yn deletedYN);
}
