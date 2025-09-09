package com.fitian.burntz.domain.channel.v1.controller;

import com.fitian.burntz.domain.channel.entity.ChannelParticipant;
import com.fitian.burntz.domain.channel.v1.dto.ChannelCreateRequest;
import com.fitian.burntz.domain.channel.service.ChannelService;
import com.fitian.burntz.domain.channel.v1.dto.ChannelListResponse;
import com.fitian.burntz.domain.member.service.MemberService;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    private final MemberService memberService;

    @PostMapping()
    public ApiResponse<Void> createChannel(
            @Valid @RequestBody ChannelCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails ) {
        channelService.createChannel(request,  userDetails);
        return ApiResponse.success(null, "채널 개설 완료.");
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<List<ChannelListResponse>>> getChannels(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long boxPk) {
        return ResponseEntity.ok(ApiResponse.success(channelService.getChannels(userDetails, boxPk),"채널 목록 반환 완료."));
    }


    @GetMapping("/{channelPk}/participants")
    public ResponseEntity<ApiResponse<List<ChannelParticipant>>> getParticipants(
            @PathVariable Long channelPk,
            @AuthenticationPrincipal CustomUserDetails userDetails ) {
        return ResponseEntity.ok(ApiResponse.success(channelService.getParticipants(userDetails, channelPk),"채널 참여자 목록 반환 완료."));
    }
}
