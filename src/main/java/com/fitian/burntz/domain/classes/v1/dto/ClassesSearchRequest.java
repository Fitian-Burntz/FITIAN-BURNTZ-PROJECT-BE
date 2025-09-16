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
 * @packageName : com.fitian.burntz.domain.classes.dto
 * @fileName : ClassesSearchRequest
 * @date : 2025-09-15
 * @description : 수업 호출 DTO 입니다
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ClassesSearchRequest", description = "수업 검색 요청")
public class ClassesSearchRequest {

    @NotNull(message = "boxPk must not be blank")
    @Schema(description = "박스 PK", example = "1")
    private Long boxPk;

    @NotNull(message = "startDate must not be blank")
    @Schema(description = "검색 시작 일자", example = "2025-09-16")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "endDate must not be blank")
    @Schema(description = "검색 종료 일자", example = "2025-09-30")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
