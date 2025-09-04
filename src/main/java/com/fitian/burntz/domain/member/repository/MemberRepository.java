package com.fitian.burntz.domain.member.repository;

import com.fitian.burntz.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByMemberId(String memberId);
    Optional<Member> findByEmail(String email);
}
