package com.fitian.burntz.domain.membership.v2.controller;

import com.fitian.burntz.domain.membership.docs.BoxHoldingPolicyDocs;
import com.fitian.burntz.domain.membership.service.BoxHoldingPolicyService;
import com.fitian.burntz.domain.membership.v2.dto.BoxHoldingPolicyRequest;
import com.fitian.burntz.domain.membership.v2.dto.BoxHoldingPolicyResponse;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/boxPk/{boxPk}/holding-policy")
@RequiredArgsConstructor
public class BoxHoldingPolicyController implements BoxHoldingPolicyDocs {

    private final BoxHoldingPolicyService boxHoldingPolicyService;

    @PutMapping
    public ResponseEntity<ApiResponse<BoxHoldingPolicyResponse>> upsertPolicy(
            @PathVariable Long boxPk,
            @RequestBody BoxHoldingPolicyRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                boxHoldingPolicyService.upsertPolicy(boxPk, request, userDetails), "홀딩 정책 설정 완료"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<BoxHoldingPolicyResponse>> getPolicy(
            @PathVariable Long boxPk,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                boxHoldingPolicyService.getPolicy(boxPk, userDetails), "조회 성공"));
    }
}
