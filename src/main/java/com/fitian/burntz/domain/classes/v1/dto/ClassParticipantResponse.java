package com.fitian.burntz.domain.classes.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "ClassParticipantResponse", description = "수업 참여자 응답")
public class ClassParticipantResponse {

    @Schema(description = "참여 PK", example = "10")
    private Long classParticipantPk;

    @Schema(description = "클래스 PK", example = "123")
    private Long classesPk;

    @Schema(description = "멤버 PK", example = "456")
    private Long memberPk;

    @Schema(description = "생성일시(서버 기준)", example = "2025-09-16T15:00:00")
    private LocalDateTime createdAt;
}
