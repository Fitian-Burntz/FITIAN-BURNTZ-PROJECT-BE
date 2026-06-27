package com.fitian.burntz.domain.channel.v2.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChannelImageUploadResponse {

    private String originalUrl;
    private String mediumUrl;
}
