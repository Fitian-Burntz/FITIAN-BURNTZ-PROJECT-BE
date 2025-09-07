package com.fitian.burntz.domain.member.repository;

import com.fitian.burntz.domain.member.entity.Member;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);
    Optional<Member> findByProviderAndMemberId(String provider, String memberId);


    /** 멤버와 연관된 Auth 모두 찾기 **/
    @Query("select m from Member m left join fetch m.auths where m.memberPk = :id")
    Optional<Member> findByIdWithAuths(@Param("id") Long id);
}
