package com.fitian.burntz.domain.auth.repository;

import com.fitian.burntz.domain.auth.entity.Auth;
import com.fitian.burntz.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, Long> {
    Optional<Auth> findByMember(Member member);
    Optional<Auth> findByDeviceIdAndMember(String deviceId, Member member);
    void deleteByMember(Member member);

    /**
     * 주어진 memberPk로 가장 최근(가장 큰 authPk) Auth 레코드 조회
     */
    Optional<Auth> findTopByMemberMemberPkOrderByAuthPkDesc(Long memberPk);

    // 정확히 이 토큰(해시) 하나만 삭제
    int deleteByMemberMemberPkAndRefreshToken(Long memberPk, String refreshToken);

    // 해당 유저의 모든 토큰 삭제
    void deleteByMemberMemberPk(Long memberPk);

    Optional<Auth> findByMember_MemberPkAndDeviceId(Long memberPk, String deviceId);

    // (member_pk, refresh_token) 조합으로 존재 여부 확인
    boolean existsByMember_MemberPkAndRefreshToken(Long memberPk, String refreshToken);

    int deleteByMemberMemberPkAndDeviceId(Long memberPk, String deviceId);

}
