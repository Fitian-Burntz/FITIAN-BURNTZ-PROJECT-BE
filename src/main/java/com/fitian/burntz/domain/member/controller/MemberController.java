package com.fitian.burntz.domain.member.controller;

import com.fitian.burntz.domain.member.dto.MemberDto;
import com.fitian.burntz.domain.member.service.MemberService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<ApiResponse<MemberDto>> updateMemberInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String gender) {

        Long memberPk = customUserDetails.getMemberPk();

        MemberDto updateResponse = memberService.updateMemberInfo(memberPk, nickname, gender);

        return ResponseEntity.ok(ApiResponse.success(updateResponse));
    }


    @DeleteMapping
    public ResponseEntity<ApiResponse<MemberDto>> removeMember(
            @AuthenticationPrincipal CustomUserDetails customUserDetails){

        Long memberPk = customUserDetails.getMemberPk();

        MemberDto removeResponse = memberService.removeMember(memberPk);

        return ResponseEntity.ok(ApiResponse.success(removeResponse,
                "Your membership withdrawal has been completed."));
    }
}