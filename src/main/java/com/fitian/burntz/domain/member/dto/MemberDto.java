package com.fitian.burntz.domain.member.dto;

import com.fitian.burntz.domain.member.entity.Member;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberDto {
    private Long memberPk;
    private String memberId;
    private String nickname;
    private String email;
    private String gender;   // enum이면 .name() 사용
    private String provider;

    public static MemberDto from(Member member) {
        return MemberDto.builder()
                .memberPk(member.getMemberPk())
                .memberId(member.getMemberId())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .gender(member.getGender() == null ? null : member.getGender().name())
                .provider(member.getProvider())
                .build();
    }
}
