package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.member.dto.MemberCreateResult;
import com.fitian.burntz.domain.member.entity.Member;

public interface MemberService {
    MemberCreateResult getOrCreateMember(String provider, String memberId, String name, String email);
}
