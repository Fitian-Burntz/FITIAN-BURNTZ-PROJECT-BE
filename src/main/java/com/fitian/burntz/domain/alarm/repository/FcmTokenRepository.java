package com.fitian.burntz.domain.alarm.repository;

import com.fitian.burntz.domain.alarm.entity.FcmToken;
import com.fitian.burntz.global.common.entity.BaseTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.alarm.repository
 * @fileName : AlarmRepository
 * @date : 2026-01-12
 * @description : FcmToken 리포지토리입니다.
 */
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    @Query("select ft from FcmToken ft " +
            "where ft.member.memberPk = :memberPk and ft.deviceId = :deviceId and ft.deletedYN = 'N'")
    Optional<FcmToken> findTokenByMemberMemberPkAndDeviceIdAndDeletedYN(@Param("memberPk") Long memberPk, @Param("deviceId") String deviceId);

    @Query("select ft from FcmToken ft " +
            "where ft.member.memberPk = :memberPk and ft.deletedYN = 'N'")
    List<FcmToken> findTokenByMemberMemberPkAndDeletedYN(@Param("memberPk") Long memberPk);

    @Query("select ft from FcmToken ft " +
            "where ft.member.memberPk = :memberPk and token = :token and ft.deletedYN = 'N'")
    Optional<FcmToken> findTokenByTokenAndDeletedYN(@Param("memberPk") Long memberPk, @Param("token") String token);

    @Query("""
        select ft
        from FcmToken ft
        where ft.member.memberPk in :memberPks
          and ft.deletedYN = :yn
    """)
    List<FcmToken> findActiveTokensByMemberPks(@Param("memberPks") Collection<Long> memberPks,
                                               @Param("yn") BaseTime.Yn yn);
}
