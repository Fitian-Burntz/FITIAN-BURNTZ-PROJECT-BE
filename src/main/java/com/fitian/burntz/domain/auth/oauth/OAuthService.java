package com.fitian.burntz.domain.auth.oauth;


import com.fitian.burntz.domain.auth.dto.OAuthUserInfo;
import com.fitian.burntz.domain.member.dto.MemberCreateResult;
import com.fitian.burntz.domain.member.entity.Member;

public interface OAuthService {

    /**
     * provider: "apple" | "google"
     * token: id_token 또는 authorization code (클라이언트에서 전달 방식에 따라)
     */
    // 기존 token 기반 메서드(필요하면 유지)
//    Member findOrCreateUserBySocialToken(String token, String provider);

    // 새로 추가: 이미 검증된 OAuthUserInfo 를 전달하여 멤버 조회/생성
    MemberCreateResult findOrCreateUserBySocialToken(String token, String provider);
    MemberCreateResult findOrCreateUserByUserInfo(OAuthUserInfo userInfo, String provider);
}
