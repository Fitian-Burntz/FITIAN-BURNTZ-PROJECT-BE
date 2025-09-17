package com.fitian.burntz.domain.membership.v1.controller;

import com.fitian.burntz.domain.membership.v1.dto.MembershipResponse;
import com.fitian.burntz.domain.membership.service.MembershipService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}
