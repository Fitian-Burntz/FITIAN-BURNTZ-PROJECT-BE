package com.fitian.burntz.global.common.v1.controller;

import com.fitian.burntz.global.common.entity.Agreement;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.common.service.CommonService;
import com.fitian.burntz.global.common.v1.dto.AgreementCreateRequestDto;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.global.common.v1
 * @fileName : CommonController
 * @date : 2026-01-29
 * @description : 공용 컨트롤러입니다.
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/common")
public class CommonController {

    private final CommonService commonService;

    @PostMapping("/agreement")
    public ApiResponse<Void> createAgreement(
            @RequestBody AgreementCreateRequestDto dto) {
        commonService.createAgreement(dto);
        return ApiResponse.success(null, "약관 생성 완료");
    }

    @GetMapping("/agreement")
    public ApiResponse<Agreement> getAgreement(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam Long agreementPk) {
        return ApiResponse.success(commonService.getAgreement(agreementPk), "약관 조회 완료");
    }

}
