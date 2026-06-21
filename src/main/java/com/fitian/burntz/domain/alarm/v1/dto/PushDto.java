package com.fitian.burntz.domain.alarm.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author : 김관중
 * @packageName : com.fitian.burntz.domain.alarm.v1.dto
 * @fileName : PushDto
 * @date : 2026-01-13
 * @description : 기본 푸시 dto 입니다.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushDto {
    @NotBlank(message = "title must not be blank")
    @Schema(description = "title", example = "박스에 가입되었습니다!")
    private String title;

    @NotBlank(message = "body must not be blank")
    @Schema(description = "body", example = "CrossFit AID를 둘러보세요.")
    private String body;

    private String channelId;
}
