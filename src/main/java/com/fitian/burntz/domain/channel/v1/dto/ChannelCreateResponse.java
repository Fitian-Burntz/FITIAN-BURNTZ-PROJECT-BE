package com.fitian.burntz.domain.channel.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ChannelCreateResponse", description = "채널 생성 응답")
public class ChannelCreateResponse {
    @Schema(description = "생성된 채널의 PK", example = "42")
    private Long channelPk;
}
