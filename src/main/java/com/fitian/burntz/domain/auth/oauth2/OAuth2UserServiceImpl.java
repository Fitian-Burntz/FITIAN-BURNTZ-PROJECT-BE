package com.fitian.burntz.domain.auth.oauth2;

import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.member_enum.Gender;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.domain.member.service.MemberService;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import com.fitian.burntz.global.security.jwt.JwtTokenProvider;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {

    private final JwtTokenProvider jwtTokenProvider;
    private final MemberService memberService;

    @Value("${jwt.accessTokenExpirationTime}")
    private Long jwtAccessTokenExpirationTime;

    @Value("${jwt.refreshTokenExpirationTime}")
    private Long jwtRefreshTokenExpirationTime;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        provider = provider == null ? "" : provider.toLowerCase(Locale.ROOT);
        Map<String,Object> attributes = oAuth2User.getAttributes();

        //OAuth2AccessToken을 userRequest에서 꺼냄 ***
        String rawAccessToken = null; // 실제 제공되는 OAuth2 access token 문자열
        try {
            if (userRequest.getAccessToken() != null) {
                rawAccessToken = userRequest.getAccessToken().getTokenValue();
            }
        } catch (Exception e) {
            log.warn("[OAuth2UserService] unable to read access token from userRequest", e);
        }


        // memberId를 미리 선언(모든 분기에서 초기화 보장)
        String memberId = null;
        String email = null;
        String name = null;

        if ("google".equals(provider)){
            memberId = (String) attributes.get("sub"); // *** sub 우선 사용 ***
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");

            // 안전 fallback
            if (memberId == null || memberId.isBlank()) {
                // 거의 발생하지 않지만 안전장치
                memberId = UUID.randomUUID().toString();
            }
        }
        else if ("apple".equals(provider)){
            //Apple은 userinfo endpoint 대신 id_token 에 정보가 담겨온다.
            // OAuth2UserRequest 의 additionalParameters 에 id_token 이 있는 경우가 많다.
            Object idTokenObj = userRequest.getAdditionalParameters().get("id_token");
            String idToken = idTokenObj != null ? idTokenObj.toString() : null;

            if (idToken != null) {
                try {
                    SignedJWT signedJWT = SignedJWT.parse(idToken);
                    JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
                    String subject = claims.getSubject();
                    Object emailObj = claims.getClaim("email");

                    // memberId를 subject 기반으로 항상 설정 (emailObj 존재 여부와 무관)
                    if (subject != null && !subject.isEmpty()) {
                        memberId = "apple:" + subject;
                    } else {
                        memberId = "apple:" + UUID.randomUUID();
                    }

                    if (emailObj != null) {
                        email = emailObj.toString();
                    }

                    //Apple 은 최초 동의 시에만 name 을 전달함 (리다이렉트 시 추가 파라미터로).
                    // 없으면 기본값 사용
                    name = email != null ? email.split("@")[0] : "AppleUser";
                }
                catch (ParseException e) {
                    //파싱 실해 시 fallback
                    memberId = "apple:" + UUID.randomUUID();
                    name = "AppleUser";
                }
            }
            else {
                //안전하게 fallback
                memberId = "apple:" + UUID.randomUUID();
                name = "AppleUser";
            }
        }
        else {
            // 기타 소셜: 랜덤 memberId
            memberId = provider + ":" + UUID.randomUUID();
            name = (String) attributes.getOrDefault("name", "socialUser");
            email = (String) attributes.getOrDefault("email", "");
        }


        //직접 repository로 생성하지 않고 MemberService 사용하여 조회/생성 통일 ***
        Member member = memberService.getOrCreateMember(provider, memberId, name, email);

        // 인증 principal 및 JWT 생성
        CustomUserDetails principal = new CustomUserDetails(member);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                Collections.singleton(new SimpleGrantedAuthority("MEMBER")));

        String accessToken = jwtTokenProvider.generateToken(auth, jwtAccessTokenExpirationTime);
        String refreshToken = jwtTokenProvider.generateToken(auth, jwtRefreshTokenExpirationTime);

        //로그. 그리고 userRequest에서 읽은 raw access token(원본 OAuth 토큰)도 attributes에 포함 ***
        log.info("[OAuth2UserService] provider={} memberPk={} memberId={} -> generated accessToken(len={}), refreshToken(len={})",
                provider, member.getMemberPk(), memberId,
                accessToken != null ? accessToken.length() : 0,
                refreshToken != null ? refreshToken.length() : 0
        );

        if (rawAccessToken != null) {
            log.info("[OAuth2UserService] raw OAuth access token present (length={})", rawAccessToken.length());
        } else {
            log.info("[OAuth2UserService] raw OAuth access token NOT present from userRequest");
        }


        Map<String, Object> customAttributes = new HashMap<>(attributes);
        customAttributes.put("accessToken", accessToken);
        customAttributes.put("refreshToken", refreshToken);
        customAttributes.put("memberPk", member.getMemberPk());

        // "subject" 를 식별자 키로 지정 (프론트 memberPk 로 확인한 안전)
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("MEMBER")),
                customAttributes,
                "memberPk"
        );

    }
}

