package com.fitian.burntz.domain.membership.v2.controller;

import com.fitian.burntz.domain.membership.docs.MembershipHoldDocs;
import com.fitian.burntz.domain.membership.service.MembershipHoldService;
import com.fitian.burntz.domain.membership.v2.dto.MembershipHoldIdentifierRequest;
import com.fitian.burntz.domain.membership.v2.dto.MembershipHoldRequest;
import com.fitian.burntz.domain.membership.v2.dto.MembershipHoldResponse;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/boxPk/{boxPk}/membership/{memberPk}/hold")
@RequiredArgsConstructor
public class MembershipHoldController implements MembershipHoldDocs {

    private final MembershipHoldService membershipHoldService;

    @PostMapping
    public ResponseEntity<ApiResponse<MembershipHoldResponse>> createHold(
            @PathVariable Long boxPk,
            @PathVariable Long memberPk,
            @Valid @RequestBody MembershipHoldRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                membershipHoldService.createHold(boxPk, memberPk, request, userDetails), "홀딩 신청 완료"));
    }

    @PostMapping("/end")
    public ApiResponse<Void> endHold(
            @PathVariable Long boxPk,
            @PathVariable Long memberPk,
            @Valid @RequestBody MembershipHoldIdentifierRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        membershipHoldService.endHold(boxPk, memberPk, request, userDetails);
        return ApiResponse.success(null, "홀딩 종료 완료");
    }

    @PostMapping("/cancel")
    public ApiResponse<Void> cancelHold(
            @PathVariable Long boxPk,
            @PathVariable Long memberPk,
            @Valid @RequestBody MembershipHoldIdentifierRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        membershipHoldService.cancelHold(boxPk, memberPk, request, userDetails);
        return ApiResponse.success(null, "홀딩 롤백 완료");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MembershipHoldResponse>>> getHolds(
            @PathVariable Long boxPk,
            @PathVariable Long memberPk,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                membershipHoldService.getHolds(boxPk, memberPk, userDetails), "조회 성공"));
    }
}
