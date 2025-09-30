package com.fitian.burntz.domain.auth.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "소셜 로그인 시 사용자 정보를 받아오는 DTO")
public class OAuthUserInfo {
    private String memberId;            // provider 고유 id (sub)
    private String email;
    private Boolean emailVerified;
    private String nickname;      // display name
}
