package com.fitian.burntz.domain.classes.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.classes.dto
 * @fileName : ClassesJoinRequest
 * @date : 2025-09-15
 * @description : 수업 공통 DTO 입니다.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ClassesIdentifierRequest", description = "단일 수업 식별 요청")
public class ClassesIdentifierRequest {

    @NotNull(message = "classesPk must not be blank")
    @Schema(description = "클래스 PK", example = "123")
    private Long classesPk;

    @NotNull(message = "boxPk must not be blank")
    @Schema(description = "박스 PK", example = "1")
    private Long boxPk;
}
