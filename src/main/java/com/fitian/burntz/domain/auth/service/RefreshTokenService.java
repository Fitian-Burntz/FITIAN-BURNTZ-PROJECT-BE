package com.fitian.burntz.domain.auth.service;

public interface RefreshTokenService {
    /**
     * 신규 생성: memberPk 기준으로 새 Auth 생성 (기본 deviceId null)
     */
    void create(Long memberPk, String refreshToken);

    /**
     * 멤버의 refresh token을 갱신(또는 새로 생성)
     * deviceId 를 전달하면 device 바인딩 가능
     */
    void saveOrUpdateRefreshToken(Long memberPk, String newRefreshToken, String deviceId);

    /**
     * DB에 저장된 해시와 비교하여 유효하면 true 반환
     */
    boolean validateRefreshTokenForMember(Long memberPk, String refreshToken);

    void revokeRefreshToken(Long memberPk);

    /** 정확히 해당 리프레시 토큰만 삭제 (해시 매칭) */
    boolean deleteByMemberAndToken(Long memberPk, String refreshToken);

    /** 해당 유저의 모든 리프레시 토큰 삭제 */
    void deleteAllByMember(Long memberPk);

    boolean deleteByMemberAndDeviceId(Long memberPk, String deviceId);
}