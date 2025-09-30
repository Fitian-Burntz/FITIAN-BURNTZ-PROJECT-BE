package com.fitian.burntz.domain.member.controller;

import com.fitian.burntz.domain.member.docs.MemberListDocs;
import com.fitian.burntz.domain.member.dto.BoxWithMembershipDto;
import com.fitian.burntz.domain.member.dto.memberList_dto.*;
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
public class MemberListController implements MemberListDocs {

    private final MemberListService memberListService;
    private final PreconditionValidator preconditionValidator;
    private static final int MAX_PAGE_SIZE = 100;


    /**  box 멤버 역할 변경 MEMBER, MANAGER (양도 x) **/
    @Override
    @PostMapping
    public ResponseEntity<ApiResponse<UpdateMemberRoleDto>> updateMemberRole(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateMemberRoleRequest updateMemberRoleRequest
            ){

        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        UpdateMemberRoleDto updateResponse = memberListService.updateMemberRole(
                loginMemberPk, UpdateMemberRoleDto.fromRequest(updateMemberRoleRequest));

        return ResponseEntity.ok(ApiResponse.success(updateResponse, "The member's role has been successfully changed."));
    }

    /** 내 box 정보 리스트 보기 **/
    @Override
    @GetMapping("/my-boxes")
    public ResponseEntity<ApiResponse<Page<BoxWithMembershipDto>>> getMyBoxesWithMembership(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    ) {
        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);

        // 안전: 클라이언트가 큰 size를 요청하면 제한
        Pageable safePageable = preconditionValidator.limitPageable(pageable, MAX_PAGE_SIZE);

        Page<BoxWithMembershipDto> myBoxListResponsePage =
                memberListService.getMyBoxesWithMembership(loginMemberPk, safePageable);


        return ResponseEntity.ok(ApiResponse.success(myBoxListResponsePage,
                "내 Box 목록 조회 성공 (" + myBoxListResponsePage.getTotalElements() + "건)"));
    }

    /** 내 box nickname 바꾸기 **/
    @Override
    @PutMapping
    public ResponseEntity<ApiResponse<ChangeMyBoxNicknameDto>> changeMyBoxNickname(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(value = "boxPk", required = false) Long boxPk,
            @RequestParam(value = "newBoxNickname", required = false) String newBoxNickname
    ){
        Long loginMemberPk = preconditionValidator.requireLogin(customUserDetails);
        Long targetBoxPk = preconditionValidator.requireBoxPk(boxPk);
        String targetNewBoxNickname = preconditionValidator.requiredStringValue(newBoxNickname);

        ChangeMyBoxNicknameDto changeBoxNicknameResponse =
                memberListService.changeMyBoxNickname(loginMemberPk, targetBoxPk, targetNewBoxNickname);

        return ResponseEntity.ok(ApiResponse.success(changeBoxNicknameResponse,
                "Box nickname has been successfully changed."));
    }


    /** 회원 정보 단건 조회 (OWNER, MANAGER 전용) **/
    @Override
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
    @Override
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

        Page<MemberListWithMembershipDto> allMemberListResponsePage =
                memberListService.getMemberListsWithMembership(targetBoxCode, loginMemberPk, safePageable);

        return ResponseEntity.ok(ApiResponse.success(
                allMemberListResponsePage, "멤버 목록 조회 성공 (" + allMemberListResponsePage.getTotalElements() + "건)"
        ));
    }


    /** OWNER 양도
     * MANAGER 등급 회원에게만 양도가능
     * 기존 OWNER 는 MEMBER 로 강등 **/
    @Override
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
