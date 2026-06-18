package com.fitian.burntz.domain.channel.v2.docs;

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

@Tag(name = "채널 메시지 API (v2)", description = "채팅 메시지 전송을 백엔드 경유로 처리합니다.")
public interface ChannelMessageDocs {

    @Operation(summary = "채팅 메시지 전송 (v2)",
            description = "메시지를 Firestore에 저장하고 FCM 푸시 알림을 발송합니다. " +
                    "clientMessageId를 지정하면 동일 ID로 중복 전송을 방지합니다.")
    ApiResponse<MessageSendResponse> sendMessage(
            @Parameter(description = "채널 PK") @PathVariable Long channelPk,
            @Valid @RequestBody MessageSendRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails);
}
