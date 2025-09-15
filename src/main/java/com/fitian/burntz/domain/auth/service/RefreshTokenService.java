package com.fitian.burntz.domain.auth.service;

import com.fitian.burntz.global.exception.ValidationException;

/**
 * RefreshToken 관련 서비스 인터페이스.
 * AuthServiceImpl에서 사용되는 validateRefreshTokenAndDevice를 포함합니다.
 */
public interface RefreshTokenService {

    void saveOrUpdateRefreshToken(Long memberPk, String newRefreshToken, String deviceId);

    void softDeleteAllByMember(Long memberPk);

    boolean softDeleteByMemberAndDeviceId(Long memberPk, String deviceId);

    /**
     * 토큰 + deviceId 검증: 성공하면 ValidationResult를 반환, 실패 시 ValidationException 던짐.
     * AuthServiceImpl이 이 메서드를 호출하도록 역할을 이동했습니다.
     */
    ValidationResult validateRefreshTokenAndDevice(String refreshToken, String deviceId) throws ValidationException;

    public static record ValidationResult(Long memberPk, String deviceId) {}
}
