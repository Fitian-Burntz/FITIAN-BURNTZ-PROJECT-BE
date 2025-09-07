package com.fitian.burntz.global.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class HashUtil {

    private HashUtil() {}

    public static String hmacSha512Hex(String input, byte[] keyBytes) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "HmacSHA512");
            mac.init(keySpec);
            byte[] raw = mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new RuntimeException("HmacSHA512 failed", e);
        }
    }

    public static boolean matchesHmacSha512(String raw, String storedHex, byte[] keyBytes) {
        if (raw == null || storedHex == null) return false;
        String computedHex = hmacSha512Hex(raw, keyBytes);
        byte[] a = HexFormat.of().parseHex(computedHex);
        byte[] b = HexFormat.of().parseHex(storedHex);
        return MessageDigest.isEqual(a, b);
    }
}
