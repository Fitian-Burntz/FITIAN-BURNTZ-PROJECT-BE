package com.fitian.burntz.domain.classes.v1.dto;

import com.fitian.burntz.domain.classes.entity.Classes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private Classes classes;
}
