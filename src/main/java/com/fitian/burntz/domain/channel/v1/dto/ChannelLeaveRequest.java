package com.fitian.burntz.domain.channel.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.channel.v1.dto
 * @fileName : ChannelLeaveRequest
 * @date : 2025-09-10
 * @description : 채널(채팅) 참여 나가기 DTO 입니다
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ChannelLeaveRequest", description = "채널(채팅방) 참여 나가기 요청")
public class ChannelLeaveRequest {

    @NotNull(message = "ParticipantPk must not be blank")
    @Schema(description = "참여(Participant) PK", example = "987")
    private Long ParticipantPk;

}
