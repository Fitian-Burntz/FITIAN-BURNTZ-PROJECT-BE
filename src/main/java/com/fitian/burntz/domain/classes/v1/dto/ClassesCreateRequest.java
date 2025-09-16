package com.fitian.burntz.domain.classes.v1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.classes.dto
 * @fileName : ClassesCreateRequest
 * @date : 2025-09-15
 * @description : 수업 생성 DTO 입니다
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ClassesCreateRequest", description = "수업 생성 요청")
public class ClassesCreateRequest {

    @NotNull(message = "boxPk must not be blank")
    @Schema(description = "박스 PK", example = "1")
    private Long boxPk;

    @NotNull(message = "classDate must not be blank")
    @Schema(description = "수업 일자", example = "2025-09-16")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate classDate;

    @NotBlank(message = "startTime must not be blank")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "HH:mm 형식이어야 합니다")
    @Schema(description = "시작 시각", example = "09:00")
    private String startTime;

    @NotBlank(message = "endTime must not be blank")
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "HH:mm 형식이어야 합니다")
    @Schema(description = "종료 시각", example = "09:50")
    private String endTime;

    @NotNull(message = "classMemberCapacity must not be blank")
    @Min(1) @Max(99)
    @Schema(description = "정원", example = "12")
    private Integer classMemberCapacity;

    @NotBlank(message = "classTitle must not be blank")
    @Schema(description = "수업 제목", example = "CrossFit Fundamentals")
    private String classTitle;

    @Schema(description = "메모", example = "초보자 환영")
    private String classMemo;

}
