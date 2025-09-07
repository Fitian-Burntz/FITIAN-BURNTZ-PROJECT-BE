package com.fitian.burntz.domain.member.repository;

import com.fitian.burntz.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);
    Optional<Member> findByProviderAndMemberId(String provider, String memberId);
}
