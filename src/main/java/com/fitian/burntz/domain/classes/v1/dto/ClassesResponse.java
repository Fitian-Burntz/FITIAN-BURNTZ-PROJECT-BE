package com.fitian.burntz.domain.classes.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.classes.v1.dto
 * @fileName : ClassesResponse
 * @date : 2025-09-24
 * @description : 수업 반환 DTO 입니다
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ClassesResponse", description = "수업 조회 응답")
public class ClassesResponse {

    @Schema(description = "수업 PK", example = "1")
    private Long classesPk;

    @Schema(description = "수업 일자", example = "2025-09-16")
    private LocalDate classDate;

    @Schema(description = "시작 시각", example = "09:00")
    private String startTime;

    @Schema(description = "종료 시각", example = "09:50")
    private String endTime;

    @Schema(description = "정원", example = "12")
    private Integer classMemberCapacity;

    @Schema(description = "수업 제목", example = "CrossFit Fundamentals")
    private String classTitle;

    @Schema(description = "메모", example = "초보자 환영")
    private String classMemo;
}
