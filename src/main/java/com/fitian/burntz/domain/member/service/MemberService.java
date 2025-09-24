package com.fitian.burntz.domain.member.service;

import com.fitian.burntz.domain.member.dto.MemberCreateResult;
import com.fitian.burntz.domain.member.dto.MemberDto;

public interface MemberService {
    MemberCreateResult getOrCreateMember(String provider, String memberId, String name, String email);

    MemberDto updateMemberInfo(Long memberPk, String newNickname, String newGender);

    MemberDto withdrawMember(Long memberPk);

    /** 최근에 접속한 Box 업데이트 **/
    void updateLastVisitedBox(Long memberPk, Long boxPk);
}
