package com.fitian.burntz.domain.admin.payment.controller;

import com.fitian.burntz.domain.admin.dto.AdminAccount;
import com.fitian.burntz.domain.admin.dto.response.AdminBoxesResponse;
import com.fitian.burntz.domain.admin.dto.response.AdminPurchaseLogResponse;
import com.fitian.burntz.domain.admin.payment.service.AdminPaymentService;
import com.fitian.burntz.global.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminPaymentController {

    private final AdminAccount adminAccount;
    private final AdminPaymentService adminPaymentService;

    @GetMapping("/boxes")
    public ApiResponse<List<AdminBoxesResponse>> getBoxes(HttpServletRequest request) {
        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 박스 목록 조회 불가");
            return ApiResponse.success(List.of());
        }
        return ApiResponse.success(adminPaymentService.getBoxes());
    }

    @GetMapping("/purchase/log/{boxPk}")
    public ApiResponse<AdminPurchaseLogResponse> getPurchaseLog(
            @PathVariable Long boxPk,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 구매 로그 조회 불가 (boxPk={})", boxPk);
            return ApiResponse.success(null);
        }

        return ApiResponse.success(adminPaymentService.getPurchaseLog(boxPk));
    }

    @PutMapping("/purchase/status/{boxPk}/{status}")
    public ApiResponse<Void> updatePurchaseStatus(
            @PathVariable Long boxPk,
            @PathVariable String status,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 구독 상태 변경 불가 (boxPk={})", boxPk);
            return ApiResponse.success(null);
        }

        adminPaymentService.updatePurchaseStatus(boxPk, status);
        return ApiResponse.success(null);
    }
}
