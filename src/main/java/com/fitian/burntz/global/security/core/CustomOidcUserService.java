package com.fitian.burntz.global.security.core;

import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.member_enum.Gender;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.domain.member.service.MemberService;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import com.fitian.burntz.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

//    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.accessTokenExpirationTime}")
    private Long jwtAccessTokenExpirationTime;

    @Value("${jwt.refreshTokenExpirationTime}")
    private Long jwtRefreshTokenExpirationTime;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        // delegate to default OidcUserService to get the standard OidcUser
        OidcUser oidcUser = super.loadUser(userRequest);

        // attributes (id_token/userinfo claims)
        Map<String, Object> attributes = new HashMap<>(oidcUser.getAttributes());

        // provider 확보 및 정규화 (소문자 통일) ***
        String provider = userRequest.getClientRegistration().getRegistrationId();
        provider = provider == null ? "" : provider.toLowerCase(Locale.ROOT);

        // 식별자 (memberId) 결정: Google은 sub 우선 사용 (email 사용하지 않음)
        String memberId = null;
        String email = (String) attributes.get("email");
        String name = (String) attributes.getOrDefault("name", "socialUser");

        Object subObj = attributes.get("sub"); // sub 기반 memberId 사용
        if (subObj != null) {
            memberId = subObj.toString();
        } else {
            // 안전 fallback: UUID (provider 별로 조합해서 조회하므로 중복 위험 적음)
            memberId = UUID.randomUUID().toString();
        }


        //MemberService 사용하여 조회/생성 통일 ***
        Member member = memberService.getOrCreateMember(provider, memberId, name, email);

        // 세션/인증용 CustomUserDetails와 Authentication 객체(토큰 생성용)
        CustomUserDetails principal = new CustomUserDetails(member);

        // JWT 생성 (너의 JwtTokenProvider 사용)
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                principal, null, Collections.singleton(new SimpleGrantedAuthority("MEMBER"))
        );
        String accessToken = jwtTokenProvider.generateToken(auth, jwtAccessTokenExpirationTime);
        String refreshToken = jwtTokenProvider.generateToken(auth, jwtRefreshTokenExpirationTime);

        // custom attributes 추가
        attributes.put("accessToken", accessToken);
        attributes.put("refreshToken", refreshToken);
        attributes.put("memberPk", member.getMemberPk());

        // 새로운 OidcUserInfo로 대체(추가 속성 포함)
        OidcUserInfo newUserInfo = new OidcUserInfo(attributes);

        // 새 DefaultOidcUser 생성: 기존 authorities, idToken 유지, userInfo 교체, nameAttributeKey는 "memberPk" 사용
        OidcIdToken idToken = oidcUser.getIdToken();
        DefaultOidcUser newOidcUser = new DefaultOidcUser(oidcUser.getAuthorities(), idToken, newUserInfo, "memberPk");

        return newOidcUser;
    }
}
