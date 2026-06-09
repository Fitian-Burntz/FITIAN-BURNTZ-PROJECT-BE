package com.fitian.burntz.domain.member.dto;

import com.fitian.burntz.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.Objects;

@Getter
@Builder
@Schema(description = "member 정보 수정 및 탈퇴 시 멤버 정보 반환용 DTO")
public class MemberDto {
    private Long memberPk;
    private String memberId;
    private String nickname;
    private String email;
    private String gender;   // enum이면 .name() 사용
    private String provider;
    private Long lastVisitedBoxPk;

    public static MemberDto from(Member member) {
        Objects.requireNonNull(member, "member required.");

        return MemberDto.builder()
                .memberPk(member.getMemberPk())
                .memberId(member.getMemberId())
                .nickname(member.getNickname())
                .email(member.getEmail())
                .gender(member.getGender() == null ? null : member.getGender().name())
                .provider(member.getProvider())
                .lastVisitedBoxPk(member.getLastVisitedBoxPk())
                .build();
    }
}
