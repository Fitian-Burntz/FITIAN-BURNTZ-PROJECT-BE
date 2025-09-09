package com.fitian.burntz.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fitian.burntz.domain.member.dto.MemberDto;
import com.fitian.burntz.domain.member.entity.Member;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private JwtTokenPair jwtTokenPair;
//    private String FirebaseCustomToken;
    private MemberDto member;

    @JsonProperty("isNewMember")
    private boolean newMember;

    private String deviceId;
}