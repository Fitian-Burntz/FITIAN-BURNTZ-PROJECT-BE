package com.fitian.burntz.domain.auth.oauth2;

import com.fitian.burntz.domain.member.entity.Member;
import com.fitian.burntz.domain.member.member_enum.Gender;
import com.fitian.burntz.domain.member.repository.MemberRepository;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import com.fitian.burntz.global.security.jwt.JwtTokenProvider;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.accessTokenExpirationTime}")
    private Long jwtAccessTokenExpirationTime;

    @Value("${jwt.refreshTokenExpirationTime}")
    private Long jwtRefreshTokenExpirationTime;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId(); // "google" or "apple"
        Map<String,Object> attributes = oAuth2User.getAttributes();

        // memberId를 미리 선언(모든 분기에서 초기화 보장)
        String memberId = null;
        String email = null;
        String name = null;

        if ("google".equals(provider)){
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            memberId = email;
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


        //회원이 없으면 자동 가입
        Member member = memberRepository.findByMemberId(memberId).orElse(null);

        if (member == null) {
            // 람다 캡쳐 이슈 회피: 로컬 변수로 미리 준비
            String signupName = (name != null) ? name : "소셜 유저";
            String signupEmail = (email != null) ? email : "";
            Gender signupGender = Gender.OTHERS; // enum 이름이 정확한지 확인하세요 (OTHER / OTHERS)

            Member newMember = Member.create(
                    memberId,
                    signupName,
                    signupEmail,
                    signupGender
            );

            try {
                member = memberRepository.save(newMember);
            } catch (DataIntegrityViolationException ex) {
                // 동시성으로 인해 다른 스레드가 동일 memberId로 이미 삽입했을 수 있음.
                // 이 경우엔 DB에서 다시 조회해서 existing member를 사용
                member = memberRepository.findByMemberId(memberId)
                        .orElseThrow(() -> new RuntimeException("Failed to create member and no existing member found", ex));
            }
        }

        CustomUserDetails principal = new CustomUserDetails(member);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                Collections.singleton(new SimpleGrantedAuthority("MEMBER")));

        String accessToken = jwtTokenProvider.generateToken(auth, jwtAccessTokenExpirationTime);
        String refreshToken = jwtTokenProvider.generateToken(auth, jwtRefreshTokenExpirationTime);

        Map<String, Object> customAttributes = new HashMap<>(attributes);
        customAttributes.put("accessToken", accessToken);
        customAttributes.put("refreshToken", refreshToken);
        customAttributes.put("memberPk", member.getMemberPk());

        // "subject" 를 식별자 키로 지정 (프론트 memberPk 로 확인한 안전)
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("MEMBER")),
                customAttributes,
                "subject"
        );

    }
}

