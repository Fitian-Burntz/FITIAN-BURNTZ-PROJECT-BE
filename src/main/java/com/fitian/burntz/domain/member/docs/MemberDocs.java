package com.fitian.burntz.domain.member.docs;

import com.fitian.burntz.domain.member.dto.MemberDto;
import com.fitian.burntz.domain.member.dto.MemberInfoResponse;
import com.fitian.burntz.domain.member.dto.memberList_dto.BoxAndMemberListDto;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * @author : 김남이
 * @packageName : com.fitian.burntz.domain.member.docs
 * @fileName : MemberDocs
 * @date : 2025-09-30
 * @description : MemberController 스웨거 설정
 */

@Tag(name = "member 관련 api 입니다.", description = "내 정보 조회, 업데이트, 탈퇴를 수행합니다.")
public interface MemberDocs {

    @Operation(summary = "내 정보 가져오기", description = "사용자의 내 정보를 가져옵니다.")
    public ResponseEntity<ApiResponse<MemberDto>> getMyInfo(@AuthenticationPrincipal CustomUserDetails customUserDetails);

    @Operation(summary = "내가 가입한 박스 정보들 가져오기", description = "사용자가 가입중인 박스들의 요약 정보를 가져옵니다.")
    public ResponseEntity<ApiResponse<List<BoxAndMemberListDto>>> getMyBoxes(@AuthenticationPrincipal CustomUserDetails customUserDetails);

    @Operation(summary = "내 정보 수정하기", description = "사용자의 내 정보(default nickname, gender)를 수정합니다.")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> updateMemberInfo(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String gender);

    @Operation(summary = "서비스 탈퇴하기", description = "사용자가 자발적으로 서비스를 탈퇴합니다. 탈퇴하더라도 재로그인 시 계정이 다시 활성화 됩니다.(휴먼계정 개념)")
    public ResponseEntity<ApiResponse<MemberInfoResponse>> withdrawMember(
            @AuthenticationPrincipal CustomUserDetails customUserDetails);
}
