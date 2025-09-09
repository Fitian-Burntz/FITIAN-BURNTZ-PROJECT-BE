package com.fitian.burntz.domain.auth.dto;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OAuthUserInfo {
    private String memberId;            // provider 고유 id (sub)
    private String email;
    private Boolean emailVerified;
    private String nickname;      // display name
}
