package com.fitian.burntz.domain.member.controller;

import com.fitian.burntz.domain.member.dto.MemberDto;
import com.fitian.burntz.domain.member.dto.MemberInfoResponse;
import com.fitian.burntz.domain.member.service.MemberService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.common.util.PreconditionValidator;
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
    private final PreconditionValidator preconditionValidator;

    @GetMapping
    public ResponseEntity<ApiResponse<MemberDto>> getMyInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails){
        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        MemberDto getMemberResponse = memberService.getMyInfo(loginMemberPk);

        return ResponseEntity.ok(ApiResponse.success(getMemberResponse));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<MemberInfoResponse>> updateMemberInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String gender) {

        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        MemberDto updateResponse = memberService.updateMemberInfo(loginMemberPk, nickname, gender);

        return ResponseEntity.ok(ApiResponse.success(MemberInfoResponse.from(updateResponse)));
    }


    /** 멤버 자진 탈퇴 **/
    @DeleteMapping
    public ResponseEntity<ApiResponse<MemberInfoResponse>> withdrawMember(
            @AuthenticationPrincipal CustomUserDetails customUserDetails){

        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        MemberDto removeResponse = memberService.withdrawMember(loginMemberPk);

        return ResponseEntity.ok(ApiResponse.success(MemberInfoResponse.from(removeResponse),
                "Your membership withdrawal has been completed."));
    }
}