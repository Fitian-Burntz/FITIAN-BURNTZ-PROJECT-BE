package com.fitian.burntz.domain.member.controller;

import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.member.dto.memberList_dto.UpdateMemberRoleDto;
import com.fitian.burntz.domain.member.dto.memberList_dto.UpdateMemberRoleRequest;
import com.fitian.burntz.domain.member.service.MemberListService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/member-list")
public class MemberListController {

    private final MemberListService memberListService;


    /**  box 멤버 역할 변경 MEMBER, MANAGER (양도 x) **/
    // 컨트롤러에서 반드시 필요한 값 null 체크 (방어적 코드)
    @PostMapping
    public ResponseEntity<?> updateMemberRole(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateMemberRoleRequest updateMemberRoleRequest
            ){

        Long loginMemberPk = customUserDetails.getMemberPk();

        // 컨트롤러에서 빠르게 인증 확인
        if (loginMemberPk == null) {
            throw new ValidationException(ErrorCode.UNAUTHORIZED);
        }

        UpdateMemberRoleDto updateResponse = memberListService.updateMemberRole(
                loginMemberPk, UpdateMemberRoleDto.from(updateMemberRoleRequest));

        return ResponseEntity.ok(ApiResponse.success(updateResponse, "The member's role has been successfully changed."));
    }



}
