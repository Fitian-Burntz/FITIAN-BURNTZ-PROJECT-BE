package com.fitian.burntz.domain.member.dto;

import com.fitian.burntz.domain.member.entity.Member;

/**
 * Service에서 회원 생성/조회 시 반환하는 결과
 * record로 선언하면 불변이면서도 접근이 직관적임.
 */
public record MemberCreateResponse(Member member, boolean isNewMember) {}