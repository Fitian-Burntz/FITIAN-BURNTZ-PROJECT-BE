package com.fitian.burntz.domain.channel.v1.dto;

import com.fitian.burntz.domain.channel.enums.ChannelType;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "ChannelCreateRequest", description = "채널 생성 요청")
public class ChannelCreateRequest {
    @NotBlank(message = "boxCode must not be blank")
    @Schema(description = "박스 코드", example = "burntz")
    private String boxCode;

    @NotBlank(message = "channelId must not be blank")
    @Schema(description = "채널 고유 ID", example = "tia_7745523145")
    private String channelId;

    @NotBlank(message = "channelName must not be blank")
    @Schema(description = "채널 이름", example = "General")
    private String channelName;

    @NotNull(message = "type is required")
    @Schema(description = "채널 유형", example = "GROUP", allowableValues = {"group","public","notice","general"})
    private ChannelType type;

    @NotEmpty(message = "memberPks must contain at least one member")
    @Schema(description = "초대할 멤버 PK 목록", example = "[101, 202, 303]")
    private List<Long> memberPks;
}
