package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.member.entity.Member;

public interface MemberService {
    Member getOrCreateMember(String provider, String memberId, String name, String email);
}
