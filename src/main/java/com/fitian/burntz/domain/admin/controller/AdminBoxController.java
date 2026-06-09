package com.fitian.burntz.domain.admin.controller;

import com.fitian.burntz.domain.admin.dto.AdminAccount;
import com.fitian.burntz.domain.admin.dto.response.AdminBoxDetailResponse;
import com.fitian.burntz.domain.admin.dto.response.BoxActivityResponse;
import com.fitian.burntz.domain.admin.service.AdminBoxService;
import com.fitian.burntz.global.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminBoxController {

    private final AdminAccount adminAccount;
    private final AdminBoxService adminBoxService;

    @GetMapping("/boxes/{boxPk}/detail")
    public ApiResponse<AdminBoxDetailResponse> getBoxDetail(
            @PathVariable Long boxPk,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 박스 상세 조회 불가 (boxPk={})", boxPk);
            return ApiResponse.success(null);
        }

        return ApiResponse.success(adminBoxService.getBoxDetail(boxPk));
    }

    @GetMapping("/boxes/{boxPk}/members/{memberPk}/memberships")
    public ApiResponse<List<AdminBoxDetailResponse.MembershipWithHistoryInfo>> getMemberMemberships(
            @PathVariable Long boxPk,
            @PathVariable Long memberPk,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 멤버십 히스토리 조회 불가 (boxPk={}, memberPk={})", boxPk, memberPk);
            return ApiResponse.success(List.of());
        }

        return ApiResponse.success(adminBoxService.getMemberMemberships(boxPk, memberPk));
    }

    @GetMapping("/boxes/{boxPk}/wod-day")
    public ApiResponse<AdminBoxDetailResponse.WodDayInfo> getWodDay(
            @PathVariable Long boxPk,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - WOD 날짜별 조회 불가 (boxPk={})", boxPk);
            return ApiResponse.success(null);
        }

        return ApiResponse.success(adminBoxService.getWodDay(boxPk, date));
    }

    @GetMapping("/boxes/{boxPk}/classes")
    public ApiResponse<List<AdminBoxDetailResponse.ClassInfo>> getBoxClasses(
            @PathVariable Long boxPk,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 클래스 조회 불가 (boxPk={})", boxPk);
            return ApiResponse.success(List.of());
        }

        return ApiResponse.success(adminBoxService.getBoxClasses(boxPk, start, end));
    }

    @GetMapping("/boxes/{boxPk}/classes/{classesPk}/participants")
    public ApiResponse<List<AdminBoxDetailResponse.ClassParticipantInfo>> getClassParticipants(
            @PathVariable Long boxPk,
            @PathVariable Long classesPk,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 참여자 조회 불가 (classesPk={})", classesPk);
            return ApiResponse.success(List.of());
        }

        return ApiResponse.success(adminBoxService.getClassParticipants(classesPk));
    }

    @GetMapping("/boxes/{boxPk}/channels")
    public ApiResponse<List<AdminBoxDetailResponse.ChannelInfo>> getBoxChannels(
            @PathVariable Long boxPk,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 채널 조회 불가 (boxPk={})", boxPk);
            return ApiResponse.success(List.of());
        }

        return ApiResponse.success(adminBoxService.getBoxChannels(boxPk));
    }

    @GetMapping("/boxes/{boxPk}/records")
    public ApiResponse<List<AdminBoxDetailResponse.RecordInfo>> getBoxRecords(
            @PathVariable Long boxPk,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 레코드 조회 불가 (boxPk={})", boxPk);
            return ApiResponse.success(List.of());
        }

        return ApiResponse.success(adminBoxService.getBoxRecords(boxPk));
    }

    @GetMapping("/boxes/{boxPk}/activities")
    public ApiResponse<Page<BoxActivityResponse>> getBoxActivities(
            @PathVariable Long boxPk,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        if (!adminAccount.validateAccount(request)) {
            log.info("[Admin] 관리자 인증 실패 - 박스 활동 조회 불가 (boxPk={})", boxPk);
            return ApiResponse.success(null);
        }

        return ApiResponse.success(adminBoxService.getBoxActivities(boxPk, page, size));
    }
}
