package com.fitian.burntz.global.common.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.global.common.v1.dto
 * @fileName : AgreementCreateRequestDto
 * @date : 2026-01-29
 * @description : 이용약관 생성 요청 DTO 입니다.
 */

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(name = "AgreementCreateRequestDto", description = "Agreement 생성 요청")
public class AgreementCreateRequestDto {

    @Schema(description = "Agreement 언어", example="Ko")
    private String language;

    @Schema(description = "title", example="이용약관")
    private String title;

    @Schema(description = "content")
    private String content;
}
