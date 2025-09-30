package com.fitian.burntz.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fitian.burntz.domain.member.dto.MemberDto;
import com.fitian.burntz.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "login 시 사용자의 통합 정보(인증토큰 정보, 소셜로그인 회원 정보, 최초 가입여부, 기기 정보)를 반환하는 응답 DTO")
public class LoginResponse {

    private JwtTokenPair jwtTokenPair;
//    private String FirebaseCustomToken;
    private MemberDto member;

    @JsonProperty("isNewMember")
    private boolean newMember;

    private String deviceId;
}