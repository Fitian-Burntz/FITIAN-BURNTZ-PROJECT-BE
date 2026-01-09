package com.fitian.burntz.domain.channel.v1.dto;

import com.fitian.burntz.domain.channel.enums.ChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.channel.v1.dto
 * @fileName : ChannelListResponse
 * @date : 2025-09-09
 * @description : 채널 리스트 DTO 입니다
 */

@Setter
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ChannelListResponse", description = "채널 목록 응답")
public class ChannelListResponse {

    @Schema(description = "채널 PK", example = "12")
    private Long channelPk;

    @Schema(description = "채널 고유 ID", example = "tia_7745523145")
    private String channelId;

    @Schema(description = "채널 이름", example = "General")
    private String channelName;

    @Schema(description = "채널 이모지", example = "\uD83D\uDE06")
    private String channelEmoji;

    @Schema(description = "채널 유형", example = "GROUP", allowableValues = {"group","public","notice","general"})
    private ChannelType type;
}
