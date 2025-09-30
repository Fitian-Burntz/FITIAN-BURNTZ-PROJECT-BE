package com.fitian.burntz.domain.member.dto;

import com.fitian.burntz.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Service에서 회원 생성/조회 시 반환하는 결과
 * record로 선언하면 불변이면서도 접근이 직관적임.
 */

@Schema(description = "member 생성 및 조회 시 member 정보와 최초 가입자 여부만을 반환하는 record")
public record MemberCreateResult(Member member, boolean isNewMember) {}