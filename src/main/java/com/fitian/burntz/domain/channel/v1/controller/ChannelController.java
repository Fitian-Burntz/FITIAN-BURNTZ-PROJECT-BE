package com.fitian.burntz.domain.channel.v1.controller;

import com.fitian.burntz.domain.channel.v1.dto.ChannelCreateRequest;
import com.fitian.burntz.domain.channel.service.ChannelService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.channel.controller
 * @fileName : ChannelController
 * @date : 2025-09-08
 * @description : 채널(채팅) 컨트롤러 입니다.
 */

@RestController
@RequestMapping("/api/v1/channels")
@RequiredArgsConstructor
public class ChannelController {

    private final ChannelService channelService;

    @PostMapping()
    public ResponseEntity<ApiResponse<Void>> createChannel(
            @Valid @RequestBody ChannelCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
            ) {
        channelService.createChannel(request,  userDetails);
        return ResponseEntity.ok(ApiResponse.success(null, "채널 개설 완료."));
    }
}
