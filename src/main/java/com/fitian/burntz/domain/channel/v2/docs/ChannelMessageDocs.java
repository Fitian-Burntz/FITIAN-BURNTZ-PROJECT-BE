package com.fitian.burntz.domain.channel.v2.docs;

import com.fitian.burntz.domain.channel.v2.dto.ChannelImageUploadResponse;
import com.fitian.burntz.domain.channel.v2.dto.MessageSendRequest;
import com.fitian.burntz.domain.channel.v2.dto.MessageSendResponse;
import com.fitian.burntz.global.common.response.ApiResponse;
import com.fitian.burntz.global.security.core.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "채널 메시지 API (v2)", description = "채팅 메시지 전송을 백엔드 경유로 처리합니다.")
public interface ChannelMessageDocs {

    @Operation(summary = "채팅 메시지 전송 (v2)",
            description = "메시지를 Firestore에 저장하고 FCM 푸시 알림을 발송합니다. " +
                    "clientMessageId를 지정하면 동일 ID로 중복 전송을 방지합니다. " +
                    "text와 imageUrl 중 하나는 반드시 포함해야 합니다.")
    ApiResponse<MessageSendResponse> sendMessage(
            @Parameter(description = "채널 PK") @PathVariable Long channelPk,
            @Valid @RequestBody MessageSendRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(summary = "채널 이미지 업로드 (v2)",
            description = "이미지를 S3에 업로드하고 원본 URL과 medium URL을 반환합니다. " +
                    "최대 20MB. 채팅에서는 mediumUrl을 표시하고, 이미지 클릭 시 originalUrl을 사용하세요. " +
                    "반환된 URL을 메시지 전송 API의 imageUrl / imageOriginalUrl에 담아 전송합니다.")
    ApiResponse<ChannelImageUploadResponse> uploadImage(
            @Parameter(description = "채널 PK") @PathVariable Long channelPk,
            @Parameter(description = "업로드할 이미지 파일 (최대 20MB)") @RequestParam("image") MultipartFile image,
            @AuthenticationPrincipal CustomUserDetails userDetails);
}
