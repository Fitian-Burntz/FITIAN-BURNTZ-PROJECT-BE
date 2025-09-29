package com.fitian.burntz.domain.member.controller;

import com.fitian.burntz.domain.member.dto.memberList_dto.ChangeOwnerSuccessDto;
import com.fitian.burntz.domain.member.dto.memberList_dto.MemberListWithMembershipDto;
import com.fitian.burntz.domain.member.dto.memberList_dto.UpdateMemberRoleDto;
import com.fitian.burntz.domain.member.dto.memberList_dto.UpdateMemberRoleRequest;
import com.fitian.burntz.domain.member.service.MemberListService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.common.util.PreconditionValidator;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/member-list")
public class MemberListController {

    private final MemberListService memberListService;
    private final PreconditionValidator preconditionValidator;
    private static final int MAX_PAGE_SIZE = 100;


    /**  box 멤버 역할 변경 MEMBER, MANAGER (양도 x) **/
    // 컨트롤러에서 반드시 필요한 값 null 체크 (방어적 코드)
    @PostMapping
    public ResponseEntity<?> updateMemberRole(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateMemberRoleRequest updateMemberRoleRequest
            ){

        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        UpdateMemberRoleDto updateResponse = memberListService.updateMemberRole(
                loginMemberPk, UpdateMemberRoleDto.fromRequest(updateMemberRoleRequest));

        return ResponseEntity.ok(ApiResponse.success(updateResponse, "The member's role has been successfully changed."));
    }


    /** 회원 정보 단건 조회 (OWNER, MANAGER 전용) **/
    @GetMapping
    public ResponseEntity<ApiResponse<MemberListWithMembershipDto>> getMemberWithMembership(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(value = "boxPk", required = false) Long boxPk,
            @RequestParam(value = "memberPk", required = false) Long memberPk
    ){
        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);
        Long targetBoxPk = preconditionValidator.requireBoxPk(boxPk);
        Long targetMemberPk = preconditionValidator.requireMemberPk(memberPk);

        MemberListWithMembershipDto memberWithMembershipResponse = memberListService
                .getMemberWithMembership(targetBoxPk, loginMemberPk, targetMemberPk);

        return ResponseEntity.ok(ApiResponse.success(memberWithMembershipResponse));
    }


    /** boxCode 로 해당 box 의 모든 memberList 를 membership 과 함께 조회 **/
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<Page<MemberListWithMembershipDto>>> getAllBoxMemberList(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(value = "boxCode", required = false) String boxCode,
            @PageableDefault(page = 0, size = 20)
            @SortDefault(sort = "boxNickname", direction = Sort.Direction.ASC)
            Pageable pageable
    ){
        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        String targetBoxCode = preconditionValidator.requireBoxCode(boxCode);

        // 안전: 클라이언트가 큰 size를 요청하면 제한
        Pageable safePageable = preconditionValidator.limitPageable(pageable, MAX_PAGE_SIZE);

        Page<MemberListWithMembershipDto> AllMemberListResponsePage =
                memberListService.getMemberListsWithMembership(targetBoxCode, loginMemberPk, safePageable);

        return ResponseEntity.ok(ApiResponse.success(
                AllMemberListResponsePage, "멤버 목록 조회 성공 (" + AllMemberListResponsePage.getTotalElements() + "건)"
        ));
    }


    @PostMapping("/assignment")
    public ResponseEntity<ApiResponse<ChangeOwnerSuccessDto>> changeOwner(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(value = "memberPk",  required = false) Long memberPk,
            @RequestParam(value = "boxPk", required = false) Long boxPk
    ){
        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);
        Long targetMemberPk = preconditionValidator.requireMemberPk(memberPk);
        Long targetBoxPk = preconditionValidator.requireBoxPk(boxPk);

        ChangeOwnerSuccessDto changeOwnerResponse = memberListService.changeOwnerForBox(
                loginMemberPk, targetMemberPk, targetBoxPk
        );

        return ResponseEntity.ok(ApiResponse.success(
                changeOwnerResponse, "The transfer of owner rights has been successfully completed.")
        );
    }

}
