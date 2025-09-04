package com.fitian.burntz.global.security.core;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberRole {

    MEMBER("일반회원"),
    ADMIN("관리자");

    private String role;

    private final String code;
}
