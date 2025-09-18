package com.fitian.burntz.domain.auth.repository;

import com.fitian.burntz.domain.auth.entity.Auth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<Auth, Long> {

    boolean existsByMember_MemberPkAndRefreshToken(Long memberPk, String refreshToken);

    // -------------------------------------------------
    // Native upsert (Postgres) - ON CONFLICT 사용
    // -------------------------------------------------
    @Modifying(clearAutomatically = true)
    @Query(value = """
      INSERT INTO burntz.auth (member_pk, device_id, refresh_token, created_at, updated_at, deleted_yn)
      VALUES (:memberPk, :deviceId, :refreshToken, now(), now(), 'N')
      ON CONFLICT (member_pk, device_id)
      DO UPDATE SET refresh_token = EXCLUDED.refresh_token, updated_at = now(), deleted_yn = 'N'
      """, nativeQuery = true)
    void upsertAuth(@Param("memberPk") Long memberPk,
                    @Param("deviceId") String deviceId,
                    @Param("refreshToken") String refreshToken);


    // -------------------------------------------------
    // Bulk soft-delete (native) — device 단위
    // -------------------------------------------------
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE burntz.auth SET refresh_token = NULL, deleted_yn = 'Y', updated_at = now() WHERE member_pk = :memberPk AND device_id = :deviceId", nativeQuery = true)
    int softDeleteByMemberPkAndDeviceIdNative(@Param("memberPk") Long memberPk, @Param("deviceId") String deviceId);

    // -------------------------------------------------
    // Bulk soft-delete (native) — 모든 기기
    // -------------------------------------------------
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE burntz.auth SET refresh_token = NULL, deleted_yn = 'Y', updated_at = now() WHERE member_pk = :memberPk", nativeQuery = true)
    int softDeleteAllByMemberPkNative(@Param("memberPk") Long memberPk);
}
