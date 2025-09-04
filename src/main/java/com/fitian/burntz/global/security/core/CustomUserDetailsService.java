package com.fitian.burntz.global.security.core;

import com.fitian.burntz.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService {

    private final MemberRepository memberRepository;

    public UserDetails loadMemberByMemberPk(Long memberPk) {
        return memberRepository.findById(memberPk)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No member: " + memberPk));

    }
}
