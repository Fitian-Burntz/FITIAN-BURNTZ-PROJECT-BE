package com.fitian.burntz.domain.member.controller;

import com.fitian.burntz.domain.member.docs.MemberDocs;
import com.fitian.burntz.domain.member.dto.MemberDto;
import com.fitian.burntz.domain.member.dto.MemberInfoResponse;
import com.fitian.burntz.domain.member.dto.memberList_dto.BoxAndMemberListDto;
import com.fitian.burntz.domain.member.service.MemberService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.common.util.PreconditionValidator;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
public class MemberController implements MemberDocs {

    private final MemberService memberService;
    private final PreconditionValidator preconditionValidator;

    /** 내 정보 가져오기 (box 관련 내 정보랑은 별개) **/
    @Override
    @GetMapping
    public ResponseEntity<ApiResponse<MemberDto>> getMyInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails){
        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        MemberDto getMemberResponse = memberService.getMyInfo(loginMemberPk);

        return ResponseEntity.ok(ApiResponse.success(getMemberResponse));
    }

    @Override
    @GetMapping("/myBoxes")
    public ResponseEntity<ApiResponse<List<BoxAndMemberListDto>>> getMyBoxes(@AuthenticationPrincipal CustomUserDetails customUserDetails){
        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        List<BoxAndMemberListDto> boxAndMemberListDtoList = memberService.getMyBoxes(loginMemberPk);

        return ResponseEntity.ok(ApiResponse.success(boxAndMemberListDtoList));
    }

    /** 내 정보 수정하기 (box nickname 수정과는 별개) **/
    @Override
    @PutMapping
    public ResponseEntity<ApiResponse<MemberInfoResponse>> updateMemberInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String gender) {

        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        MemberDto updateResponse = memberService.updateMemberInfo(loginMemberPk, nickname, gender);

        return ResponseEntity.ok(ApiResponse.success(MemberInfoResponse.from(updateResponse)));
    }


    /** 프로필 이미지 업데이트 **/
    @PatchMapping(value = "/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MemberInfoResponse>> updateProfileImage(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestPart("image") MultipartFile image) {

        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        MemberDto result = memberService.updateProfileImage(loginMemberPk, image);

        return ResponseEntity.ok(ApiResponse.success(MemberInfoResponse.from(result)));
    }

    /** 멤버 자진 탈퇴
     * 서비스 재로그인 시 계정이 다시 활성화됩니다.
     * 탈퇴회원 재가입 가능
     * 휴면계정과 비슷한 개념으로 동작 **/
    @Override
    @DeleteMapping
    public ResponseEntity<ApiResponse<MemberInfoResponse>> withdrawMember(
            @AuthenticationPrincipal CustomUserDetails customUserDetails){

        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        MemberDto removeResponse = memberService.withdrawMember(loginMemberPk);

        return ResponseEntity.ok(ApiResponse.success(MemberInfoResponse.from(removeResponse),
                "Your membership withdrawal has been completed."));
    }
}