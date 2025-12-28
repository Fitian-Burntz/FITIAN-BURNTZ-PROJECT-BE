package com.fitian.burntz.domain.channel.v1.controller;

import com.fitian.burntz.domain.channel.docs.ChannelDocs;
import com.fitian.burntz.domain.channel.entity.ChannelParticipant;
import com.fitian.burntz.domain.channel.v1.dto.*;
import com.fitian.burntz.domain.channel.service.ChannelService;
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
public class ChannelController implements ChannelDocs {

    private final ChannelService channelService;

    @PostMapping()
    @Override
    public ApiResponse<Void> createChannel(
            @Valid @RequestBody ChannelCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails ) {
        channelService.createChannel(request,  userDetails);
        return ApiResponse.success(null, "채널 개설 완료.");
    }

    @GetMapping()
    @Override
    public ResponseEntity<ApiResponse<List<ChannelListResponse>>> getChannels(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long boxPk) {
        return ResponseEntity.ok(ApiResponse.success(channelService.getChannels(userDetails, boxPk),"채널 목록 반환 완료."));
    }

    @GetMapping("/{channelPk}/enter")
    @Override
    public ResponseEntity<ApiResponse<List<ParticipantListResponse>>> getChannelEnter(
            @PathVariable Long channelPk,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        boolean canEnter = channelService.canEnterChannel(channelPk, userDetails);

        if(canEnter) {
            List<ParticipantListResponse> pList = channelService.getParticipantsInfo(channelPk, userDetails);
            return ResponseEntity.ok(ApiResponse.success(pList,"입장 가능합니다."));
        }
        return ResponseEntity.status(403).body(ApiResponse.failure("참여중인 채널이 아닙니다."));
    }

    @GetMapping("/{channelPk}/participants")
    @Override
    public ResponseEntity<ApiResponse<List<ChannelParticipant>>> getParticipants(
            @PathVariable Long channelPk,
            @AuthenticationPrincipal CustomUserDetails userDetails ) {
        return ResponseEntity.ok(ApiResponse.success(channelService.getParticipants(userDetails, channelPk),"채널 참여자 목록 반환 완료."));
    }

    @PostMapping("/inviteParticipants")
    @Override
    public ApiResponse<Void> inviteParticipants(
            @Valid @RequestBody ChannelInviteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails ) {
        channelService.inviteParticipants(request, userDetails);
        return ApiResponse.success(null, "채널 초대 완료.");
    }

    @DeleteMapping("/deleteParticipant")
    @Override
    public ApiResponse<Void> deleteParticipant(
            @Valid @RequestBody ChannelLeaveRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails ) {
        boolean ok = channelService.deleteParticipant(request, userDetails);
        if(!ok) return ApiResponse.failure("내보내기에 실패했습니다.");
        return ApiResponse.success(null, "채널 내보내기 완료.");
    }

    @DeleteMapping("/deleteChannel")
    @Override
    public ApiResponse<Void> deleteChanel(
            @Valid @RequestBody ChannelDeleteRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails ) {
        boolean ok = channelService.deleteChannel(request, userDetails);
        if(!ok) return ApiResponse.failure("삭제에 실패했습니다.");
        return ApiResponse.success(null, "채널 삭제 완료.");
    }
}
