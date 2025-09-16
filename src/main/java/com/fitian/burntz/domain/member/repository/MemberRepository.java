package com.fitian.burntz.domain.member.repository;

import com.fitian.burntz.domain.member.entity.Member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByProviderAndMemberId(String provider, String memberId);

    //닉네임 중복 체크
    boolean existsByNickname(String nickname);

    Optional<Member> findByNickname(String nickname);

}

