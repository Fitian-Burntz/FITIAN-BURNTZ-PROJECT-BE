package com.fitian.burntz.global.common.util;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;

//인스턴스화 방지
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringUtil {

    /** 문자열에 앞/뒤 공백 제거, 빈 문자열일 경우 null 반환 **/
    public static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    //기본 trim() 함수는 문자열 앞/뒤 공백 제거. 빈 문자열일 경우 빈 문자열 그대로 반환.
}
