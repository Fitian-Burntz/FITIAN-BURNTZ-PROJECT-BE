package com.fitian.burntz.domain.member.repository;

import com.fitian.burntz.domain.member.entity.Member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByProviderAndMemberId(String provider, String memberId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Member m SET m.lastVisitedBoxPk = :boxPk WHERE m.memberPk = :memberPk")
    int updateLastVisitedBoxPk(@Param("memberPk") Long memberPk,
                               @Param("boxPk") Long boxPk);

}



