package com.fitian.burntz.domain.classes.v1.dto;

import jakarta.validation.constraints.NotBlank;
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
public class ClassesUpdateRequest {
    @NotBlank(message = "boxPK must not be blank")
    private Long boxPK;

    @NotBlank(message = "ClassesPk must not be blank")
    private Long ClassesPk;

    private String startTime;

    private String endTime;

    private Integer classMemberCapacity;

    private String classTitle;

    private String classMemo;
}
