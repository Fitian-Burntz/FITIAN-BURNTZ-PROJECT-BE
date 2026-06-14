package com.fitian.burntz.domain.channel.v2.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MessageSendResponse {

    private String messageId;
    private long sentAtMillis;
}
