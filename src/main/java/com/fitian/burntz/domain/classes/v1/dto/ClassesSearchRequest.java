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
 * @fileName : ClassesSearchRequest
 * @date : 2025-09-15
 * @description : 수업 호출 DTO 입니다
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassesSearchRequest {

    @NotBlank(message = "boxPK must not be blank")
    private Long boxPK;

    @NotNull(message = "startDate must not be blank")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @NotNull(message = "endDate must not be blank")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;
}
