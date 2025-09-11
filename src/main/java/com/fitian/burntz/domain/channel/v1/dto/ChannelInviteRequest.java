package com.fitian.burntz.domain.channel.v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.channel.v1.dto
 * @fileName : ChannelInviteRequest
 * @date : 2025-09-10
 * @description : 채널 초대 요청 DTO 입니다.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChannelInviteRequest {

    @NotBlank(message = "channelPk must not be blank")
    private Long channelPk;

    @NotEmpty(message = "memberPks must contain at least one member")
    private List<Long> memberPks;
}
