package com.fitian.burntz.domain.channel.dto;

import com.fitian.burntz.domain.channel.enums.ChannelType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.channel.dto
 * @fileName : ChannelCreateRequest
 * @date : 2025-09-08
 * @description : 채널 생성 요청 dto
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelCreateRequest {
    private String boxCode;
    private String channelId;
    private String channelName;
    private ChannelType type;
    private List<Long> memberPks;
}
