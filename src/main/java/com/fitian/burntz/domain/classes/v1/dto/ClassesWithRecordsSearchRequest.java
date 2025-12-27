package com.fitian.burntz.domain.classes.v1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.classes.v1.dto
 * @fileName : ClasseswithRecordsSearchRequest
 * @date : 2025-12-27
 * @description : 기록을 포함한 클래스 검색 요청 DTO 입니다.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ClassesWithRecordsSearchRequest", description = "기록유무를 포함한 수업 검색 요청")
public class ClassesWithRecordsSearchRequest {

    @NotNull(message = "boxPk must not be blank")
    @Schema(description = "박스 PK", example = "1")
    private Long boxPk;

    @NotNull(message = "date must not be blank")
    @Schema(description = "검색 시작 일자", example = "2025-09-16")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;
}
