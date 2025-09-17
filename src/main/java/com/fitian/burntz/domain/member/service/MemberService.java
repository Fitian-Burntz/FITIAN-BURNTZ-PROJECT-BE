package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.member.dto.MemberCreateResponse;
import com.fitian.burntz.domain.member.dto.MemberDto;

public interface MemberService {
    MemberCreateResponse getOrCreateMember(String provider, String memberId, String name, String email);

    MemberDto updateMemberInfo(Long memberPk, String newNickname, String newGender);

    MemberDto removeMember(Long memberPk);
}
