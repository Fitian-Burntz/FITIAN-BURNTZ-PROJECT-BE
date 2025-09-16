package com.fitian.burntz.domain.classes.v1.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.classes.v1.dto
 * @fileName : ClassParticipantResponse
 * @date : 2025-09-16
 * @description : 수업 참여자 반환 DTO 입니다
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassParticipantResponse {
    private Long classParticipantPk;
    private Long classesPk;
    private Long memberPk;
    private LocalDateTime createdAt;
}
