package com.fitian.burntz.domain.channel.v2.controller;

import com.fitian.burntz.domain.channel.v2.docs.ChannelMessageDocs;
import com.fitian.burntz.domain.channel.service.ChannelService;
import com.fitian.burntz.domain.channel.v2.dto.ChannelImageUploadResponse;
import com.fitian.burntz.domain.channel.v2.dto.MessageSendRequest;
import com.fitian.burntz.domain.channel.v2.dto.MessageSendResponse;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v2/channels")
@RequiredArgsConstructor
public class ChannelMessageController implements ChannelMessageDocs {

    private final ChannelService channelService;

    @PostMapping("/{channelPk}/messages")
    public ApiResponse<MessageSendResponse> sendMessage(
            @PathVariable Long channelPk,
            @Valid @RequestBody MessageSendRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        MessageSendResponse response = channelService.sendMessage(channelPk, request, userDetails);
        return ApiResponse.success(response, "메시지 전송 완료.");
    }

    @PostMapping(value = "/{channelPk}/images", consumes = "multipart/form-data")
    public ApiResponse<ChannelImageUploadResponse> uploadImage(
            @PathVariable Long channelPk,
            @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        ChannelImageUploadResponse response = channelService.uploadChannelImage(channelPk, image, userDetails);
        return ApiResponse.success(response, "이미지 업로드 완료.");
    }
}
