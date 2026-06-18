package com.fitian.burntz.domain.admin.controller;

import com.fitian.burntz.domain.admin.dto.AdminAccount;
import com.fitian.burntz.domain.admin.dto.response.AdminGrowthResponse;
import com.fitian.burntz.domain.admin.service.AdminGrowthService;
import com.fitian.burntz.global.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/growth")
public class AdminGrowthController {

    private final AdminAccount adminAccount;
    private final AdminGrowthService adminGrowthService;

    @GetMapping("/new-boxes")
    public ApiResponse<List<AdminGrowthResponse.NewBoxInfo>> getRecentBoxes(
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 신규 박스 조회 불가");
            return ApiResponse.success(List.of());
        }

        return ApiResponse.success(adminGrowthService.getRecentBoxes(limit));
    }

    @GetMapping("/new-members")
    public ApiResponse<List<AdminGrowthResponse.NewMemberJoinInfo>> getRecentMemberJoins(
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 신규 멤버 유입 조회 불가");
            return ApiResponse.success(List.of());
        }

        return ApiResponse.success(adminGrowthService.getRecentMemberJoins(limit));
    }
}
