package com.fitian.burntz.domain.channel.v2.dto;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageSendRequest {

    private String text;
    private String imageUrl;
    private String imageOriginalUrl;

    private String clientMessageId;
    private String parentMessageId;

    @AssertTrue(message = "text 또는 imageUrl 중 하나는 필수입니다.")
    private boolean isContentPresent() {
        boolean hasText  = text != null && !text.isBlank();
        boolean hasImage = imageUrl != null && !imageUrl.isBlank();
        return hasText || hasImage;
    }
}
