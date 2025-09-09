package com.fitian.burntz.domain.auth.oauth;

import com.fitian.burntz.domain.auth.dto.OAuthUserInfo;

public interface AppleApiClient {

    /**
     * 입력: id_token (JWT) — 검증(서명/iss/aud/exp) 후 OAuthUserInfo 반환
     */
    OAuthUserInfo getUserInfoFromIdToken(String idToken);
}
