package com.fitian.burntz.domain.auth.repository;

import com.fitian.burntz.domain.auth.entity.Auth;
import com.fitian.burntz.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, Long> {
    Optional<Auth> findByMember(Member member);
    Optional<Auth> findByDeviceIdAndMember(String deviceId, Member member);
    void deleteByMember(Member member);
}
