package com.fitian.burntz.domain.channel.v2.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageSendRequest {

    @NotBlank
    private String text;

    private String clientMessageId;
    private String parentMessageId;
}
