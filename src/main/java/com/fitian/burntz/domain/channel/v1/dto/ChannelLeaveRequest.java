package com.fitian.burntz.domain.channel.v1.dto;

import jakarta.validation.constraints.NotBlank;
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
public class ChannelLeaveRequest {

    @NotBlank(message = "ParticipantPk must not be blank")
    private Long ParticipantPk;

}
