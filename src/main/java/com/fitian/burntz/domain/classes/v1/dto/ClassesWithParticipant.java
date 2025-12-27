package com.fitian.burntz.domain.classes.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.classes.v1.dto
 * @fileName : ClassesWithRecords
 * @date : 2025-12-27
 * @description : 클래스와 레코드를 한번에 반환하는 클래스입니다.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ClassesWithParticipant", description = "수업 및 참여자 응답")
public class ClassesWithParticipant {
    @Schema(description = "수업 PK", example = "1")
    private Long classesPk;

    @Schema(description = "수업 일자", example = "2025-09-16")
    private LocalDate classDate;

    @Schema(description = "시작 시각", example = "09:00")
    private String startTime;

    @Schema(description = "기록유무를 포함한 참여자 리스트")
    private List<ParticipantWithRecord> participantWithRecordList;
}
