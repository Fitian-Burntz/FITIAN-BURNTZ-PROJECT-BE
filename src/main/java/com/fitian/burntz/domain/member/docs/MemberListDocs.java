package com.fitian.burntz.domain.member.docs;

import com.fitian.burntz.domain.member.dto.BoxWithMembershipDto;
import com.fitian.burntz.domain.member.dto.memberList_dto.*;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author : 김남이
 * @packageName : com.fitian.burntz.domain.member.docs
 * @fileName : MemberListDocs
 * @date : 2025-09-30
 * @description : MemberListController 스웨거 설정
 */

@Tag(name = "memberList 관련 api 입니다.", description = "회원 정보 조회, 역할 변경, 권한 양도")
public interface MemberListDocs {

    @Operation(summary = "회원 등급 변경", description = "box 회원의 등급을 GUEST, MEMBER, MANAGER 로 변경합니다.")
    public ResponseEntity<ApiResponse<UpdateMemberRoleDto>> updateMemberRole(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Valid @RequestBody UpdateMemberRoleRequest updateMemberRoleRequest
    );

    @Operation(summary = "내 박스 정보 단건 조회",
            description = "사용자가 속한 특정 box 의 정보를 membership 정보와 함께 단건으로 보여줍니다." +
                    "내 box 단건 조회 시 member 의 lastVisitedBoxPk 정보가 자동으로 업데이트 됩니다.")
    public ResponseEntity<ApiResponse<BoxWithMembershipDto>> getMyBoxWithMembership(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(value = "boxPk", required = false) Long boxPk
    );

    @Operation(summary = "내 박스 정보 리스트 보기", description = "사용자가 속해 있는 box 정보를 membership 정보와 함께 리스트로 보여줍니다.")
    public ResponseEntity<ApiResponse<Page<BoxWithMembershipDto>>> getMyBoxListWithMembership(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @PageableDefault(page = 0, size = 20) Pageable pageable
    );

    @Operation(summary = "내 박스 닉네임 변경", description = "사용자의 boxNickName 별 닉네임을 변경할 수 있도록 합니다.")
    public ResponseEntity<ApiResponse<ChangeMyBoxNicknameDto>> changeMyBoxNickname(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(value = "boxPk", required = false) Long boxPk,
            @RequestParam(value = "newBoxNickname", required = false) String newBoxNickname
    );

    @Operation(summary = "회원의 박스 및 멤버십 정보 단건 조회",
            description = "OWNER 나 MANAGER 가 box 회원의 통합 정보(boxNickName, role, membership)를 단건 조회할 수 있습니다.")
    public ResponseEntity<ApiResponse<MemberListWithMembershipDto>> getMemberWithMembership(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(value = "boxPk", required = false) Long boxPk,
            @RequestParam(value = "memberPk", required = false) Long memberPk
    );

    @Operation(summary = "박스의 전체 회원 리스트를 조회",
            description = "OWNER 나 MANAGER 가 box 회원의 전체의 통합 정보(boxNickName, role, membership)를 오름차순 리스트 페이징으로 조회합니다.")
    public ResponseEntity<ApiResponse<Page<MemberListWithMembershipDto>>> getAllBoxMemberList(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(value = "boxCode", required = false) String boxCode,
            @PageableDefault(page = 0, size = 20)
            @SortDefault(sort = "boxNickname", direction = Sort.Direction.ASC)
            Pageable pageable
    );


    @Operation(summary = "OWNER 양도",
            description = "OWNER 권한을 MANAGER 에게 양도합니다. 권한 양도 시 기존 OWNER 는 즉시 MEMBER 로 강등됩니다.")
    public ResponseEntity<ApiResponse<ChangeOwnerSuccessDto>> changeOwner(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(value = "memberPk",  required = false) Long memberPk,
            @RequestParam(value = "boxPk", required = false) Long boxPk
    );

    @Operation(summary = "box 의 memberList 에서 회원 삭제",
            description = "OWNER 가 box 의 memberList 에서 memberPk 해당하는 회원을 삭제 처리 합니다.")
    public ResponseEntity<ApiResponse<RemoveMemberListDto>>  removeMemberList(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(value = "memberListPk", required = false) Long memberListPk,
            @RequestParam(value = "boxPk", required = false) Long boxPk
    );
}
