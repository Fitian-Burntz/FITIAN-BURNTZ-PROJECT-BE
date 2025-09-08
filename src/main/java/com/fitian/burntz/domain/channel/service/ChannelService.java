package com.fitian.burntz.domain.channel.service;

import com.fitian.burntz.domain.channel.dto.ChannelCreateRequest;
import com.fitian.burntz.global.security.core.CustomUserDetails;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.channel.service
 * @fileName : ChannelService
 * @date : 2025-09-08
 * @description : 채널(채팅) 추상체 입니다.
 */

public interface ChannelService {
    void createChannel(ChannelCreateRequest request, CustomUserDetails userDetails);
}
