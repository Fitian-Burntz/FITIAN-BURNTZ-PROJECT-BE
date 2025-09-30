package com.fitian.burntz.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "")
public record MemberInfoResponse (
        Long memberPk,
        String memberId,
        String nickname,
        String email,
        String gender,
        String provider
) {
    public static MemberInfoResponse from(MemberDto memberDto) {
        return new MemberInfoResponse(
                memberDto.getMemberPk(),
                memberDto.getMemberId(),
                memberDto.getNickname(),
                memberDto.getEmail(),
                memberDto.getGender(),
                memberDto.getProvider()
        );
    }
}
