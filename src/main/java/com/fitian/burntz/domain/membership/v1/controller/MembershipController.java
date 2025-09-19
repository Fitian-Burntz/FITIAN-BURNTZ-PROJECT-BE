package com.fitian.burntz.domain.membership.v1.controller;

import com.fitian.burntz.domain.membership.v1.dto.*;
import com.fitian.burntz.domain.membership.service.MembershipService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.membership.controller
 * @fileName : MembershipController
 * @date : 2025-09-17
 * @description : 멤버십 컨트롤러입니다.
 */

@RestController
@RequestMapping("/api/v1/boxPk/{boxPk}/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final MembershipService membershipService;

    @GetMapping("/{memberPk}")
    public ResponseEntity<ApiResponse<MembershipResponse>> getMembership(
            @PathVariable Long boxPk,
            @PathVariable Long memberPk,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(membershipService.getMembership(boxPk, memberPk, userDetails)));
    }

    @PostMapping("/{memberPk}")
    public ApiResponse<Void> createMembership(
            @PathVariable Long boxPk,
            @PathVariable Long memberPk,
            @Valid @RequestBody MembershipCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        membershipService.createMembership(boxPk, memberPk, request, userDetails);
        return ApiResponse.success(null,"회원권 생성 완료");
    }

    @PutMapping("/{memberPk}")
    public ApiResponse<Void> udpateMembership(
            @PathVariable Long boxPk,
            @PathVariable Long memberPk,
            @Valid @RequestBody MembershipUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        membershipService.updateMembership(boxPk, memberPk, request, userDetails);
        return ApiResponse.success(null,"회원권 변경 완료");
    }

    @DeleteMapping("/{memberPk}")
    public ApiResponse<Void> deleteMembership(
            @PathVariable Long boxPk,
            @PathVariable Long memberPk,
            @Valid @RequestBody MembershipIdentifierRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails){
        membershipService.deleteMembership(boxPk, memberPk, request, userDetails);
        return ApiResponse.success(null,"회원권 삭제 완료");
    }

    @GetMapping("/{memberPk}/log")
    public ResponseEntity<ApiResponse<List<MembershipHistoryResponse>>> getMembershipHistory(
            @PathVariable Long boxPk,
            @PathVariable Long memberPk,
            @Valid @RequestBody MembershipIdentifierRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                membershipService.getMembershipHistory(boxPk, memberPk, request, userDetails),"조회 성공"
        ));
    }
}
