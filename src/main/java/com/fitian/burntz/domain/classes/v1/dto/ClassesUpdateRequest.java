package com.fitian.burntz.domain.classes.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.classes.v1.dto
 * @fileName : ClassesUpdateRequest
 * @date : 2025-09-16
 * @description : 수업 변경 DTO 입니다
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ClassesUpdateRequest", description = "수업 수정 요청")
public class ClassesUpdateRequest {
    @NotNull(message = "boxPK must not be blank")
    @Schema(description = "박스 PK", example = "123")
    private Long boxPk;

    @NotNull(message = "classesPk must not be blank")
    @Schema(description = "클래스 PK", example = "1")
    private Long classesPk;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "HH:mm 형식이어야 합니다")
    @Schema(description = "시작 시각", example = "09:00")
    private String startTime;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "HH:mm 형식이어야 합니다")
    @Schema(description = "종료 시각", example = "09:50")
    private String endTime;

    @Min(1) @Max(99)
    @Schema(description = "정원", example = "12")
    private Integer classMemberCapacity;

    @Schema(description = "수업 제목", example = "CrossFit Fundamentals")
    private String classTitle;

    @Schema(description = "메모", example = "초보자 환영")
    private String classMemo;
}
