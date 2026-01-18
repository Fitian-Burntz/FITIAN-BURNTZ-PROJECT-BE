package com.fitian.burntz.domain.alarm.v1.controller;

import com.fitian.burntz.domain.alarm.docs.AlarmDocs;
import com.fitian.burntz.domain.alarm.entity.FcmToken;
import com.fitian.burntz.domain.alarm.service.AlarmService;
import com.fitian.burntz.domain.alarm.v1.dto.FcmTokenCreateRequest;
import com.fitian.burntz.domain.alarm.v1.dto.MessagePushRequest;
import com.fitian.burntz.domain.alarm.v1.dto.MessagePushResponse;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
public class AlarmController implements AlarmDocs {

    private final AlarmService alarmService;

    @PostMapping()
    public ApiResponse<Void> upsertToken(
            @Valid @RequestBody FcmTokenCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails ) {
        FcmToken token = alarmService.upsertToken(userDetails, request);
        return ApiResponse.success(null, "TokenPk : "+token.getTokenPk()+" 토큰 등록.");
    }

    @PostMapping("/push-message")
    public ApiResponse<MessagePushResponse> pushMessage(
            @Valid @RequestBody MessagePushRequest request) {
        return ApiResponse.success(alarmService.dispatch(request));
    }
}
