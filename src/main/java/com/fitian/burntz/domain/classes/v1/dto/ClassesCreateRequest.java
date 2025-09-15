package com.fitian.burntz.domain.classes.v1.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ClassesCreateRequest {

    @NotBlank(message = "boxPK must not be blank")
    private Long boxPK;

    @NotNull(message = "classDate must not be blank")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate classDate;

    @NotBlank(message = "startTime must not be blank")
    private String startTime;

    @NotBlank(message = "endTime must not be blank")
    private String endTime;

    @NotBlank(message = "classMemberCapacity must not be blank")
    private Integer classMemberCapacity;

    @NotBlank(message = "classTitle must not be blank")
    private String classTitle;

    private String classMemo;

}
