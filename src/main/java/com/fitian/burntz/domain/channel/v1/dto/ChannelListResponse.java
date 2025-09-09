package com.fitian.burntz.domain.channel.v1.dto;

import com.fitian.burntz.domain.channel.enums.ChannelType;
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
public class ChannelListResponse {
    private Long channelPk;
    private String channelId;
    private String channelName;
    private ChannelType type;
}
