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
 * @fileName : ClassesDeleteRequest
 * @date : 2025-11-30
 * @description : 일자별 클래스 삭제 요청 DTO 입니다.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ClassesDeleteRequest", description = "일자별 클래스 삭제 요청")
public class ClassesDeleteRequest {

    @NotNull(message = "boxPk must not be blank")
    @Schema(description = "박스 PK", example = "1")
    private Long boxPk;

    @NotNull(message = "classDate must not be blank")
    @Schema(description = "수업 일자", example = "2025-09-16")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate classDate;

}
