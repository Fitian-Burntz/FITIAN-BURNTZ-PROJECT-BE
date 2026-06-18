package com.fitian.burntz.domain.locker.v2.controller;

import com.fitian.burntz.domain.locker.service.LockerService;
import com.fitian.burntz.domain.locker.v2.dto.*;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v2/boxes/{boxPk}/lockers")
@RequiredArgsConstructor
public class LockerController {

    private final LockerService lockerService;

    @PostMapping
    public ApiResponse<Void> createLocker(
            @PathVariable Long boxPk,
            @Valid @RequestBody LockerCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        lockerService.createLocker(boxPk, request, userDetails);
        return ApiResponse.success(null, "사물함 등록 완료");
    }

    @GetMapping
    public ApiResponse<List<LockerResponse>> getLockers(
            @PathVariable Long boxPk,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(lockerService.getLockers(boxPk, userDetails));
    }

    @DeleteMapping("/{lockerPk}")
    public ApiResponse<Void> deleteLocker(
            @PathVariable Long boxPk,
            @PathVariable Long lockerPk,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        lockerService.deleteLocker(boxPk, lockerPk, userDetails);
        return ApiResponse.success(null, "사물함 삭제 완료");
    }

    @PostMapping("/{lockerPk}/usage")
    public ApiResponse<Void> assignLocker(
            @PathVariable Long boxPk,
            @PathVariable Long lockerPk,
            @Valid @RequestBody LockerAssignRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        lockerService.assignLocker(boxPk, lockerPk, request, userDetails);
        return ApiResponse.success(null, "사물함 배정 완료");
    }

    @DeleteMapping("/{lockerPk}/usage")
    public ApiResponse<Void> revokeLocker(
            @PathVariable Long boxPk,
            @PathVariable Long lockerPk,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        lockerService.revokeLocker(boxPk, lockerPk, userDetails);
        return ApiResponse.success(null, "사물함 이용권 해제 완료");
    }

    @GetMapping("/me")
    public ApiResponse<MyLockerResponse> getMyLocker(
            @PathVariable Long boxPk,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(lockerService.getMyLocker(boxPk, userDetails));
    }
}
