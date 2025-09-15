package com.fitian.burntz.global.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * 안전한 로그 출력을 위한 유틸: 원문을 그대로 찍지 않고 SHA-256 해시의 접두사만 반환.
 */
public final class SecureLogUtil {

    private SecureLogUtil() {}

    public static String sha256Prefix(String input, int prefixLen) {
        return getString(input, prefixLen);
    }

    public static String getString(String input, int prefixLen) {
        if (input == null) return "null";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(input.getBytes(StandardCharsets.UTF_8));
            String hex = HexFormat.of().formatHex(h);
            return hex.substring(0, Math.min(prefixLen, hex.length()));
        } catch (Exception e) {
            return "err";
        }
    }

    public static String maskedIfNull(String s) {
        return (s == null || s.isBlank()) ? "(none)" : "(present)";
    }
}
