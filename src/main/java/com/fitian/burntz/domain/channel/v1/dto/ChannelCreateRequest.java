package com.fitian.burntz.domain.channel.v1.dto;

import com.fitian.burntz.domain.channel.enums.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank(message = "boxCode must not be blank")
    private String boxCode;

    @NotBlank(message = "channelId must not be blank")
    private String channelId;

    @NotBlank(message = "channelName must not be blank")
    private String channelName;

    @NotNull(message = "type is required")
    private ChannelType type;

    @NotEmpty(message = "memberPks must contain at least one member")
    private List<Long> memberPks;
}
