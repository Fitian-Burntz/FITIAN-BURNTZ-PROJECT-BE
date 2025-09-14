package com.fitian.burntz.domain.auth.service;

import com.fitian.burntz.domain.auth.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Map;

public interface AuthService {
    LoginResponse loginWithSocial(String socialToken, String provider, String deviceId);

    void logoutCurrentDevice(String refreshToken, String deviceId);

    void logoutAllDevices(String anyToken);

    Map<String, Object> refreshTokenBased(String refreshToken, String deviceId);
}
