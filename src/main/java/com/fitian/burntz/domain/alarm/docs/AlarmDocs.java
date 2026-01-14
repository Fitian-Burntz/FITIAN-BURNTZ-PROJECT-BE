package com.fitian.burntz.domain.alarm.docs;

import com.fitian.burntz.domain.alarm.v1.dto.FcmTokenCreateRequest;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.alarm.docs
 * @fileName : AlarmDocs
 * @date : 2026-01-14
 * @description : Alarm 관련 Swagger 문서
 */

@Tag(name = "Alarm 및 Push 관련 api 입니다.")
public interface AlarmDocs {
    @Operation(summary = "토큰 생성 및 업데이트", description = "해당 유저와 기기를 바탕으로 Fcm 토큰을 생성 및 업데이트 합니다.")
    public ApiResponse<Void> upsertToken(
            @Valid @RequestBody FcmTokenCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails);
}
