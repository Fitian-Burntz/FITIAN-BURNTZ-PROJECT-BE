package com.fitian.burntz.domain.alarm.v1.controller;

import com.fitian.burntz.domain.alarm.entity.FcmToken;
import com.fitian.burntz.domain.alarm.service.AlarmService;
import com.fitian.burntz.domain.alarm.service.PushService;
import com.fitian.burntz.domain.alarm.v1.dto.FcmTokenCreateRequest;
import com.fitian.burntz.domain.alarm.v1.dto.PushDto;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.alarm.controller
 * @fileName : AlarmController
 * @date : 2026-01-12
 * @description : FCM 알람을 담당하는 컨트롤러입니다.
 */

@RestController
@RequestMapping("/api/v1/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;
    private final PushService pushService;

    @PostMapping()
    public ApiResponse<Void> upsertToken(
            @Valid @RequestBody FcmTokenCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails ) {
        FcmToken token = alarmService.upsertToken(userDetails, request);
        PushDto dto = new PushDto("새 토큰 등록 완료.", "token : "+token.getToken());
        pushService.notifyUser(userDetails.getMemberPk(), dto);
        return ApiResponse.success(null, "토큰 등록.");
    }
}
