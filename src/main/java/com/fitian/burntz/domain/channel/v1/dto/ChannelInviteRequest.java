package com.fitian.burntz.domain.channel.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
@Schema(name = "ChannelInviteRequest", description = "채널에 멤버를 초대하는 요청")
public class ChannelInviteRequest {

    @NotNull(message = "channelPk must not be blank")
    @Schema(description = "채널 PK", example = "12")
    private Long channelPk;

    @NotEmpty(message = "memberPks must contain at least one member")
    @Schema(description = "초대할 멤버 PK 목록", example = "[101, 202, 303]")
    private List<Long> memberPks;
}
