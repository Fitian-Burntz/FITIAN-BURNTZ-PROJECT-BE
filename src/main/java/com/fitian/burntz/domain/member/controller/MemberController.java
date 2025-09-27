package com.fitian.burntz.domain.member.controller;

import com.fitian.burntz.domain.member.dto.MemberDto;
import com.fitian.burntz.domain.member.dto.MemberInfoResponse;
import com.fitian.burntz.domain.member.service.MemberService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PutMapping
    public ResponseEntity<ApiResponse<MemberInfoResponse>> updateMemberInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String gender) {

        Long memberPk = customUserDetails.getMemberPk();

        MemberDto updateResponse = memberService.updateMemberInfo(memberPk, nickname, gender);

        return ResponseEntity.ok(ApiResponse.success(MemberInfoResponse.from(updateResponse)));
    }


    /** 멤버 자진 탈퇴 **/
    @DeleteMapping
    public ResponseEntity<ApiResponse<MemberInfoResponse>> withdrawMember(
            @AuthenticationPrincipal CustomUserDetails customUserDetails){

        Long memberPk = customUserDetails.getMemberPk();

        MemberDto removeResponse = memberService.withdrawMember(memberPk);

        return ResponseEntity.ok(ApiResponse.success(MemberInfoResponse.from(removeResponse),
                "Your membership withdrawal has been completed."));
    }
}