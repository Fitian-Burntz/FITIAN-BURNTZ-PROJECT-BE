package com.fitian.burntz.domain.channel.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.channel.v1.dto
 * @fileName : ChannelDeleteRequest
 * @date : 2025-12-28
 * @description : 채널 삭제 요청 DTO 입니다.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ChannelDeleteRequest", description = "채널 삭제 요청")
public class ChannelDeleteRequest {
    @NotNull(message = "channelPk must not be blank")
    @Schema(description = "channel PK", example = "15")
    private Long channelPk;
}
