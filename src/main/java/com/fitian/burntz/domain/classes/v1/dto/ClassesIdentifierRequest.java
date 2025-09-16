package com.fitian.burntz.domain.classes.v1.dto;

import jakarta.validation.constraints.NotBlank;
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
public class ClassesIdentifierRequest {

    @NotBlank(message = "classesPK must not be blank")
    private Long classesPK;

    @NotBlank(message = "boxPK must not be blank")
    private Long boxPK;
}
