package com.fitian.burntz.domain.auth.oauth;

import com.fitian.burntz.domain.auth.dto.OAuthUserInfo;

public interface GoogleApiClient {
    /**
     * 입력: accessToken (또는 id_token 처리 버전을 만들 수도 있음)
     * - 여기서는 access token 으로 userinfo endpoint 호출
     */
    OAuthUserInfo getUserInfo(String accessToken);
}
