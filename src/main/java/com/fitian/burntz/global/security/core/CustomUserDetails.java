package com.fitian.burntz.global.security.core;

import com.fitian.burntz.domain.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Member member;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("MEMBER"));
    }

    public Long getMemberPk() {
        return member.getMemberPk();
    }

    public String getMemberId() {return member.getMemberId();}

    @Override
    public String getPassword() {
        return ""; // 소셜 로그인 전용이므로 빈 문자열 허용
    }

    //getUsername() 시 nickname 정보 반환
    @Override
    public String getUsername() {
        return member.getNickname();
    }



    @Override
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
