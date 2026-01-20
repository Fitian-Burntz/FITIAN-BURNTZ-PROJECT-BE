package com.fitian.burntz.domain.alarm.v1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.alarm.v1.dto
 * @fileName : MessagePushRequest
 * @date : 2026-01-18
 * @description : 채팅 메시지 푸시 요청 dto 입니다.
 */

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessagePushRequest {

    @NotBlank
    @JsonProperty("boxId")
    private String boxCode;

    @NotBlank
    private String channelId;

    private String channelName;

    @NotBlank
    private String messageId; // Firestore message docId or unique messageId

    @NotNull
    private Long sentAtMillis;   // Cloud Function 그대로

    private Long senderId;        // memberPk or memberListPk
    private String boxNickname;   // sender's boxNickname
    private Long memberListPk;    // nullable

    private String text;          // 알림용 텍스트
    private String type;          // MESSAGE / IMAGE / SYSTEM ...
}
