package com.fitian.burntz.domain.member.controller;

import com.fitian.burntz.domain.box.repository.BoxRepository;
import com.fitian.burntz.domain.member.dto.memberList_dto.MemberListWithMembershipDto;
import com.fitian.burntz.domain.member.dto.memberList_dto.UpdateMemberRoleDto;
import com.fitian.burntz.domain.member.dto.memberList_dto.UpdateMemberRoleRequest;
import com.fitian.burntz.domain.member.service.MemberListService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.exception.ErrorCode;
import com.fitian.burntz.global.exception.ValidationException;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.fitian.burntz.global.common.util.StringUtil.trimToNull;

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

        Long loginMemberPk = customUserDetails == null ? null : customUserDetails.getMemberPk();

        // 컨트롤러에서 빠르게 인증 확인
        if (loginMemberPk == null) {
            throw new ValidationException(ErrorCode.UNAUTHORIZED);
        }

        UpdateMemberRoleDto updateResponse = memberListService.updateMemberRole(
                loginMemberPk, UpdateMemberRoleDto.from(updateMemberRoleRequest));

        return ResponseEntity.ok(ApiResponse.success(updateResponse, "The member's role has been successfully changed."));
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
        Long loginMemberPk = customUserDetails == null ? null : customUserDetails.getMemberPk();

        if (loginMemberPk == null) {
            throw new ValidationException(ErrorCode.UNAUTHORIZED);
        }

        boxCode = trimToNull(boxCode);

        if (boxCode == null) {
            throw new ValidationException(ErrorCode.MISSING_REQUIRED_FIELD);
        }

        // 안전: 클라이언트가 큰 size를 요청하면 제한
        int maxSize = 100;
        pageable = PageRequest.of(pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), maxSize),
                pageable.getSort());

        Page<MemberListWithMembershipDto> AllMemberListResponsePage =
                memberListService.getMemberListsWithMembership(boxCode, loginMemberPk, pageable);

        return ResponseEntity.ok(ApiResponse.success(
                AllMemberListResponsePage, "멤버 목록 조회 성공 (" + AllMemberListResponsePage.getTotalElements() + "건)"
        ));
    }

}
