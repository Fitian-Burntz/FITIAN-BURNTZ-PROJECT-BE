package com.fitian.burntz.domain.member.controller;

import com.fitian.burntz.domain.member.dto.MemberDto;
import com.fitian.burntz.domain.member.service.MemberService;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/update")
    public MemberDto updateMemberInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String gender) {

        Long memberPk = customUserDetails.getMemberPk();

        return memberService.updateMemberInfo(memberPk, nickname, gender);
    }
}